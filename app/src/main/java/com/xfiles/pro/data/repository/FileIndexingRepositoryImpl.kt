package com.xfiles.pro.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.data.local.database.dao.FileMetadataTuple
import com.xfiles.pro.data.local.database.dao.IndexedFileDao
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.data.worker.FileIndexingWorker
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.IndexingState
import com.xfiles.pro.domain.model.StorageEvent
import com.xfiles.pro.domain.repository.FileIndexingRepository
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance implementation of [FileIndexingRepository] providing:
 * - Room persistent indexing
 * - SQLite FTS full-text search with prefix matching and substring fallback
 * - Incremental sync avoiding redundant disk rescans
 * - Automatic real-time synchronization with [StorageRepository] events
 * - Background periodic execution via [WorkManager]
 */
class FileIndexingRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatchersProvider,
    private val indexedFileDao: IndexedFileDao,
    private val storageRepository: StorageRepository,
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(context),
    private val safManager: SafManager = SafManager(context),
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)
) : FileIndexingRepository {

    private val _indexingState = MutableStateFlow(IndexingState())
    override val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    private val indexingMutex = Mutex()
    private val isCancelledFlag = AtomicBoolean(false)
    private var activeIndexingJob: Job? = null

    init {
        // Automatically listen to all storage operations (create, update, delete, move)
        // and immediately reflect them into the Room database and FTS index.
        externalScope.launch {
            storageRepository.storageEvents.collect { event ->
                handleStorageEvent(event)
            }
        }
    }

    override fun getIndexedFilesCount(): Flow<Long> {
        return indexedFileDao.getCountFlow()
    }

    override fun getFilesByParent(parentPath: String): Flow<List<FileItem>> {
        return indexedFileDao.getByParentPath(parentPath).map { entities ->
            entities.map { it.toFileItem() }
        }.flowOn(dispatchers.io)
    }

    override fun searchFiles(query: String, fileType: FileType?): Flow<List<FileItem>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return flow { emit(emptyList()) }
        }

        // Clean characters for FTS MATCH syntax to prevent SQLite syntax errors
        val sanitizedFts = trimmed.replace(Regex("[*\"'():;^~{}\\[\\]]"), " ").trim()
        val ftsMatchQuery = if (sanitizedFts.isNotEmpty()) {
            sanitizedFts.split("\\s+".toRegex()).joinToString(" ") { "$it*" }
        } else {
            ""
        }

        val categoryFilter = fileType?.name

        val ftsFlow = if (ftsMatchQuery.isNotEmpty()) {
            if (categoryFilter != null) {
                indexedFileDao.searchFtsWithCategory(ftsMatchQuery, categoryFilter)
            } else {
                indexedFileDao.searchFts(ftsMatchQuery)
            }
        } else {
            flow { emit(emptyList()) }
        }

        val likeFlow = indexedFileDao.searchByNameLike(trimmed)

        // Combine FTS results with LIKE substring results to support full Arabic/English matching
        return combine(ftsFlow, likeFlow) { ftsList, likeList ->
            val combined = LinkedHashMap<String, IndexedFileEntity>()
            // FTS results take precedence
            for (entity in ftsList) {
                if (categoryFilter == null || entity.category.equals(categoryFilter, ignoreCase = true)) {
                    combined[entity.path] = entity
                }
            }
            // Append LIKE results for partial mid-word matches
            for (entity in likeList) {
                if (!combined.containsKey(entity.path)) {
                    if (categoryFilter == null || entity.category.equals(categoryFilter, ignoreCase = true)) {
                        combined[entity.path] = entity
                    }
                }
            }

            combined.values.map { it.toFileItem() }
        }.flowOn(dispatchers.io)
    }

    override suspend fun runIndexing(forceRescan: Boolean): Result<Int> = withContext(dispatchers.io) {
        if (!indexingMutex.tryLock()) {
            return@withContext Result.failure(IllegalStateException("Indexing already in progress"))
        }

        isCancelledFlag.set(false)
        activeIndexingJob = coroutineContext[Job]

        try {
            _indexingState.update {
                it.copy(
                    isIndexing = true,
                    progress = 0f,
                    error = null
                )
            }

            var totalIndexed = 0
            val volumesResult = storageRepository.getStorageVolumes()
            val volumes = (volumesResult as? ResultWrapper.Success)?.data ?: emptyList()

            val volumeCount = volumes.size.coerceAtLeast(1)

            for ((index, volume) in volumes.withIndex()) {
                ensureActive()
                if (isCancelledFlag.get()) break

                _indexingState.update {
                    it.copy(
                        currentVolume = volume.name,
                        progress = index.toFloat() / volumeCount
                    )
                }

                val volumeFile = File(volume.path)
                if (volumeFile.exists() && volumeFile.canRead()) {
                    val count = indexVolumeDirectory(volume.id, volumeFile, forceRescan)
                    totalIndexed += count
                }
            }

            // Also index media items from MediaStore
            if (!isCancelledFlag.get()) {
                indexMediaStoreItems()
            }

            val finalCount = indexedFileDao.getCount()
            _indexingState.update {
                it.copy(
                    isIndexing = false,
                    indexedCount = finalCount,
                    progress = 1.0f,
                    currentVolume = null
                )
            }

            Result.success(totalIndexed)
        } catch (c: CancellationException) {
            _indexingState.update {
                it.copy(
                    isIndexing = false,
                    error = "Indexing cancelled"
                )
            }
            Result.failure(c)
        } catch (t: Throwable) {
            _indexingState.update {
                it.copy(
                    isIndexing = false,
                    error = t.message ?: "Indexing failed"
                )
            }
            Result.failure(t)
        } finally {
            indexingMutex.unlock()
            activeIndexingJob = null
        }
    }

    override fun cancelIndexing() {
        isCancelledFlag.set(true)
        activeIndexingJob?.cancel()
        _indexingState.update {
            it.copy(isIndexing = false)
        }
    }

    private suspend fun indexVolumeDirectory(
        volumeId: String,
        rootDir: File,
        forceRescan: Boolean
    ): Int {
        if (forceRescan) {
            indexedFileDao.deleteByVolume(volumeId)
        }

        // Fetch existing metadata for incremental sync: path -> (lastModified, size)
        val existingMap = indexedFileDao.getMetadataByVolume(volumeId).associateBy { it.path }
        val visitedPaths = HashSet<String>()
        val verifiedParentDirectories = HashSet<String>()
        val batchToInsert = ArrayList<IndexedFileEntity>(250)
        var indexedCount = 0

        val queue = ArrayDeque<File>()
        queue.add(rootDir)

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            if (isCancelledFlag.get()) break

            val current = queue.removeFirst()
            val files = current.listFiles() ?: continue
            // Directory was successfully read and enumerated
            verifiedParentDirectories.add(current.absolutePath)

            for (file in files) {
                currentCoroutineContext().ensureActive()
                if (isCancelledFlag.get()) break

                val path = file.absolutePath
                visitedPaths.add(path)

                val isDir = file.isDirectory
                val lastModified = file.lastModified()
                val size = if (isDir) 0L else file.length()

                val existing = existingMap[path]
                val needsUpdate = existing == null || existing.lastModified != lastModified || existing.size != size

                if (needsUpdate) {
                    val ext = if (isDir) "" else FileExtensions.getExtension(file.name)
                    val type = FileExtensions.detectFileType(ext, isDir)
                    val parentPath = file.parent ?: rootDir.absolutePath

                    val entity = IndexedFileEntity(
                        id = 0L, // Auto-generated
                        name = file.name,
                        path = path,
                        parentPath = parentPath,
                        size = size,
                        lastModified = lastModified,
                        isDirectory = isDir,
                        extension = ext,
                        mimeType = FileExtensions.getMimeType(ext),
                        category = type.name,
                        source = "STORAGE",
                        volumeId = volumeId,
                        canRead = file.canRead(),
                        canWrite = file.canWrite(),
                        createdAt = 0L,
                        lastIndexedAt = System.currentTimeMillis()
                    )
                    batchToInsert.add(entity)
                    indexedCount++

                    if (batchToInsert.size >= 200) {
                        indexedFileDao.insertAll(batchToInsert)
                        batchToInsert.clear()
                    }
                }

                if (isDir && !file.name.startsWith(".")) {
                    queue.add(file)
                }
            }
        }

        // Flush remaining inserts
        if (batchToInsert.isNotEmpty()) {
            indexedFileDao.insertAll(batchToInsert)
            batchToInsert.clear()
        }

        // Incremental deletion: ONLY if scan completed normally (never on cancelled/interrupted scan)
        // and strictly for files whose parent directories were actually scanned and verified
        if (!isCancelledFlag.get()) {
            val toDelete = existingMap.values.filter { tuple ->
                val parent = File(tuple.path).parent ?: ""
                verifiedParentDirectories.contains(parent) && !visitedPaths.contains(tuple.path)
            }.map { it.path }

            if (toDelete.isNotEmpty()) {
                for (chunk in toDelete.chunked(200)) {
                    indexedFileDao.deleteByPaths(chunk)
                }
            }
        }

        return indexedCount
    }

    private suspend fun indexMediaStoreItems() {
        try {
            val mediaFiles = ArrayList<IndexedFileEntity>()
            // Query images, audio, and video via MediaStoreManager
            val images = mediaStoreManager.queryMedia(FileType.IMAGE)
            val audio = mediaStoreManager.queryMedia(FileType.AUDIO)
            val video = mediaStoreManager.queryMedia(FileType.VIDEO)

            val allMedia = images + audio + video
            for (item in allMedia) {
                // Deduplication: prevent duplicate rows if file was already indexed via storage path or matching name+size
                if (indexedFileDao.getByPath(item.path) != null) {
                    continue
                }
                if (indexedFileDao.findByNameAndSize(item.name, item.size) != null) {
                    continue
                }

                val entity = IndexedFileEntity(
                    name = item.name,
                    path = item.path,
                    parentPath = item.path.substringBeforeLast('/', ""),
                    size = item.size,
                    lastModified = item.lastModified,
                    isDirectory = false,
                    extension = item.extension,
                    mimeType = FileExtensions.getMimeType(item.extension),
                    category = item.fileType.name,
                    source = "MEDIA_STORE",
                    volumeId = "primary",
                    canRead = item.canRead,
                    canWrite = item.canWrite,
                    lastIndexedAt = System.currentTimeMillis()
                )
                mediaFiles.add(entity)
            }

            if (mediaFiles.isNotEmpty()) {
                for (chunk in mediaFiles.chunked(200)) {
                    indexedFileDao.insertAll(chunk)
                }
            }
        } catch (_: Throwable) {
            // MediaStore query failure should not break entire indexing
        }
    }

    private suspend fun handleStorageEvent(event: StorageEvent) {
        when (event) {
            is StorageEvent.Created -> onFileCreatedOrUpdated(event.item, event.source, event.volumeId)
            is StorageEvent.Updated -> onFileCreatedOrUpdated(event.item)
            is StorageEvent.Deleted -> onFileDeleted(event.path)
            is StorageEvent.Moved -> onFileMoved(event.oldPath, event.newItem)
        }
    }

    override suspend fun onFileCreatedOrUpdated(item: FileItem, source: String, volumeId: String): Unit = withContext(dispatchers.io) {
        val parentPath = if (item.path.startsWith("content://")) {
            item.path.substringBeforeLast('/', "")
        } else {
            File(item.path).parent ?: ""
        }

        val existing = indexedFileDao.getByPath(item.path)
        val entity = IndexedFileEntity(
            id = existing?.id ?: 0L,
            name = item.name,
            path = item.path,
            parentPath = parentPath,
            size = item.size,
            lastModified = if (item.lastModified > 0) item.lastModified else System.currentTimeMillis(),
            isDirectory = item.isDirectory,
            extension = item.extension,
            mimeType = FileExtensions.getMimeType(item.extension),
            category = item.fileType.name,
            source = source,
            volumeId = volumeId,
            canRead = item.canRead,
            canWrite = item.canWrite,
            createdAt = existing?.createdAt ?: 0L,
            lastIndexedAt = System.currentTimeMillis()
        )
        indexedFileDao.insertOrReplace(entity)
        Unit
    }

    override suspend fun onFileDeleted(path: String): Unit = withContext(dispatchers.io) {
        indexedFileDao.deleteByPath(path)
        // Also delete any child files if it was a directory
        indexedFileDao.deleteByParentPath(path)
        Unit
    }

    override suspend fun onFileMoved(oldPath: String, newItem: FileItem): Unit = withContext(dispatchers.io) {
        indexedFileDao.deleteByPath(oldPath)
        indexedFileDao.deleteByParentPath(oldPath)
        onFileCreatedOrUpdated(newItem)
        Unit
    }

    override suspend fun clearIndex(volumeId: String?): Unit = withContext(dispatchers.io) {
        if (volumeId != null) {
            indexedFileDao.deleteByVolume(volumeId)
        } else {
            indexedFileDao.clearAll()
        }
        Unit
    }

    override fun scheduleBackgroundIndexing() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<FileIndexingWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FileIndexingWorker.UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    private fun IndexedFileEntity.toFileItem(): FileItem {
        val type = runCatching { FileType.valueOf(category) }.getOrDefault(
            if (isDirectory) FileType.FOLDER else FileExtensions.detectFileType(extension, false)
        )
        return FileItem(
            name = name,
            path = path,
            uriString = if (path.startsWith("content://")) path else null,
            size = size,
            lastModified = lastModified,
            isDirectory = isDirectory,
            fileType = type,
            extension = extension,
            isHidden = name.startsWith("."),
            canRead = canRead,
            canWrite = canWrite
        )
    }
}
