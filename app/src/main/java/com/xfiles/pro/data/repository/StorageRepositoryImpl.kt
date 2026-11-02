package com.xfiles.pro.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.SafPermission
import com.xfiles.pro.domain.model.StorageEvent
import com.xfiles.pro.domain.model.StorageVolume
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * StorageRepository implementation integrating SAF, MediaStore, and File APIs with defensive error handling.
 */
class StorageRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatchersProvider,
    private val safManager: SafManager = SafManager(context),
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(context)
) : StorageRepository {

    private val _storageEvents = MutableSharedFlow<StorageEvent>(extraBufferCapacity = 128)
    override val storageEvents: Flow<StorageEvent> = _storageEvents.asSharedFlow()

    override suspend fun getStorageVolumes(): ResultWrapper<List<StorageVolume>> = withContext(dispatchers.io) {
        try {
            val volumeList = mutableListOf<StorageVolume>()
            val primaryDir = Environment.getExternalStorageDirectory()
            val primaryStat = runCatching { StatFs(primaryDir.absolutePath) }.getOrNull()

            val primaryTotal = primaryStat?.let { it.blockCountLong * it.blockSizeLong } ?: primaryDir.totalSpace
            val primaryFree = primaryStat?.let { it.availableBlocksLong * it.blockSizeLong } ?: primaryDir.freeSpace

            volumeList.add(
                StorageVolume(
                    id = "primary",
                    name = "Internal Storage",
                    path = primaryDir.absolutePath,
                    totalBytes = primaryTotal,
                    freeBytes = primaryFree,
                    isPrimary = true,
                    isRemovable = false,
                    state = Environment.getExternalStorageState()
                )
            )

            // Discover external and removable storage across Android versions safely
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (storageManager != null) {
                val volumes = storageManager.storageVolumes
                for (vol in volumes) {
                    if (vol.isRemovable) {
                        val desc = vol.getDescription(context)
                        val dirPath = getStorageVolumePath(vol)

                        if (dirPath != null) {
                            val dirFile = File(dirPath)
                            val stat = runCatching { StatFs(dirPath) }.getOrNull()
                            val total = stat?.let { it.blockCountLong * it.blockSizeLong } ?: dirFile.totalSpace
                            val free = stat?.let { it.availableBlocksLong * it.blockSizeLong } ?: dirFile.freeSpace

                            volumeList.add(
                                StorageVolume(
                                    id = vol.uuid ?: dirFile.name,
                                    name = desc ?: "SD Card",
                                    path = dirPath,
                                    totalBytes = total,
                                    freeBytes = free,
                                    isPrimary = false,
                                    isRemovable = true,
                                    state = vol.state
                                )
                            )
                        }
                    }
                }
            }

            ResultWrapper.Success(volumeList)
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    private fun getStorageVolumePath(volume: android.os.storage.StorageVolume): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            volume.directory?.absolutePath
        } else {
            try {
                val method = volume.javaClass.getMethod("getPath")
                (method.invoke(volume) as? String) ?: volume.uuid?.let { "/storage/$it" }
            } catch (e: Exception) {
                volume.uuid?.let { "/storage/$it" }
            }
        }
    }

    override suspend fun listFiles(pathOrUri: String, sortOption: FileSortOption): ResultWrapper<List<FileItem>> = withContext(dispatchers.io) {
        try {
            val fileItems = when {
                pathOrUri.startsWith("content://") -> {
                    safManager.listDocumentFiles(Uri.parse(pathOrUri))
                }
                else -> {
                    val directory = File(pathOrUri)
                    if (!directory.exists()) {
                        return@withContext ResultWrapper.Error(IllegalArgumentException("Path does not exist: $pathOrUri"))
                    }
                    if (!directory.isDirectory) {
                        return@withContext ResultWrapper.Error(IllegalArgumentException("Path is not a directory: $pathOrUri"))
                    }

                    val rawFiles = directory.listFiles() ?: emptyArray()
                    rawFiles.map { file ->
                        val isDir = file.isDirectory
                        val ext = if (isDir) "" else FileExtensions.getExtension(file.name)
                        val type = FileExtensions.detectFileType(ext, isDir)

                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            uriString = null,
                            size = if (isDir) 0L else file.length(),
                            lastModified = file.lastModified(),
                            isDirectory = isDir,
                            fileType = type,
                            extension = ext,
                            isHidden = file.name.startsWith("."),
                            canRead = file.canRead(),
                            canWrite = file.canWrite()
                        )
                    }
                }
            }

            val sorted = sortFileItems(fileItems, sortOption)
            ResultWrapper.Success(sorted)
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun getFileDetails(pathOrUri: String): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                var name = "Unknown"
                var size = 0L
                var isDir = false

                val projection = arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameCol >= 0) name = cursor.getString(nameCol) ?: name
                        if (sizeCol >= 0) size = cursor.getLong(sizeCol)
                    }
                }

                val ext = FileExtensions.getExtension(name)
                val type = FileExtensions.detectFileType(ext, isDir)

                ResultWrapper.Success(
                    FileItem(
                        name = name,
                        path = pathOrUri,
                        uriString = pathOrUri,
                        size = size,
                        lastModified = System.currentTimeMillis(),
                        isDirectory = isDir,
                        fileType = type,
                        extension = ext,
                        canRead = true,
                        canWrite = true
                    )
                )
            } else {
                val file = File(pathOrUri)
                if (!file.exists()) {
                    return@withContext ResultWrapper.Error(IllegalArgumentException("File does not exist: $pathOrUri"))
                }
                val isDir = file.isDirectory
                val ext = if (isDir) "" else FileExtensions.getExtension(file.name)
                val type = FileExtensions.detectFileType(ext, isDir)

                ResultWrapper.Success(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        uriString = null,
                        size = if (isDir) 0L else file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = isDir,
                        fileType = type,
                        extension = ext,
                        isHidden = file.name.startsWith("."),
                        canRead = file.canRead(),
                        canWrite = file.canWrite()
                    )
                )
            }
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun createDirectory(parentPathOrUri: String, name: String): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            if (parentPathOrUri.startsWith("content://")) {
                val parentUri = Uri.parse(parentPathOrUri)
                val createdUri = safManager.createDirectory(parentUri, name)
                    ?: return@withContext ResultWrapper.Error(IllegalStateException("Failed to create SAF directory: $name"))

                val item = FileItem(
                    name = name,
                    path = createdUri.toString(),
                    uriString = createdUri.toString(),
                    isDirectory = true,
                    fileType = com.xfiles.pro.domain.model.FileType.FOLDER,
                    canRead = true,
                    canWrite = true
                )
                _storageEvents.tryEmit(StorageEvent.Created(item, source = "SAF"))
                ResultWrapper.Success(item)
            } else {
                val parent = File(parentPathOrUri)
                if (!parent.exists() || !parent.isDirectory) {
                    return@withContext ResultWrapper.Error(IllegalArgumentException("Invalid parent directory: $parentPathOrUri"))
                }
                val target = File(parent, name)
                if (target.exists()) {
                    return@withContext ResultWrapper.Error(IllegalStateException("Directory or file already exists: $name"))
                }
                if (!target.mkdirs()) {
                    return@withContext ResultWrapper.Error(IllegalStateException("Failed to create directory: $name"))
                }

                val item = FileItem(
                    name = target.name,
                    path = target.absolutePath,
                    isDirectory = true,
                    fileType = com.xfiles.pro.domain.model.FileType.FOLDER,
                    lastModified = target.lastModified(),
                    canRead = target.canRead(),
                    canWrite = target.canWrite()
                )
                _storageEvents.tryEmit(StorageEvent.Created(item, source = "STORAGE"))
                ResultWrapper.Success(item)
            }
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun createFile(parentPathOrUri: String, name: String, mimeType: String): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            if (parentPathOrUri.startsWith("content://")) {
                val parentUri = Uri.parse(parentPathOrUri)
                val createdUri = safManager.createFile(parentUri, name, mimeType)
                    ?: return@withContext ResultWrapper.Error(IllegalStateException("Failed to create SAF document: $name"))

                val ext = FileExtensions.getExtension(name)
                val type = FileExtensions.detectFileType(ext, false)
                val item = FileItem(
                    name = name,
                    path = createdUri.toString(),
                    uriString = createdUri.toString(),
                    isDirectory = false,
                    fileType = type,
                    extension = ext,
                    canRead = true,
                    canWrite = true
                )
                _storageEvents.tryEmit(StorageEvent.Created(item, source = "SAF"))
                ResultWrapper.Success(item)
            } else {
                val parent = File(parentPathOrUri)
                if (!parent.exists() || !parent.isDirectory) {
                    return@withContext ResultWrapper.Error(IllegalArgumentException("Invalid parent directory: $parentPathOrUri"))
                }
                val target = File(parent, name)
                if (target.exists()) {
                    return@withContext ResultWrapper.Error(IllegalStateException("File already exists: $name"))
                }
                if (!target.createNewFile()) {
                    return@withContext ResultWrapper.Error(IllegalStateException("Failed to create file: $name"))
                }

                val ext = FileExtensions.getExtension(target.name)
                val type = FileExtensions.detectFileType(ext, false)
                val item = FileItem(
                    name = target.name,
                    path = target.absolutePath,
                    isDirectory = false,
                    fileType = type,
                    extension = ext,
                    lastModified = target.lastModified(),
                    canRead = target.canRead(),
                    canWrite = target.canWrite()
                )
                _storageEvents.tryEmit(StorageEvent.Created(item, source = "STORAGE"))
                ResultWrapper.Success(item)
            }
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun copyFile(
        sourcePathOrUri: String,
        targetParentPathOrUri: String,
        targetName: String?,
        resolution: FileConflictResolution,
        onProgress: ((bytesCopied: Long, totalBytes: Long) -> Unit)?
    ): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            // 1. Resolve source input stream and size
            val sourceInfo = openSource(sourcePathOrUri)
                ?: return@withContext ResultWrapper.Error(IllegalArgumentException("Cannot open source: $sourcePathOrUri"))

            val resolvedName = targetName ?: sourceInfo.name

            // 2. Resolve destination path and handle conflicts
            val finalTargetName = resolveTargetName(targetParentPathOrUri, resolvedName, resolution)
            if (finalTargetName == null) {
                // SKIP conflict resolution chosen
                sourceInfo.stream.close()
                return@withContext getFileDetails(File(targetParentPathOrUri, resolvedName).absolutePath)
            }

            // 3. Safe copy via temporary file (.tmp)
            val parentFile = File(targetParentPathOrUri)
            val tempFile = File(parentFile, ".tmp_${System.currentTimeMillis()}_$finalTargetName")
            val finalTargetFile = File(parentFile, finalTargetName)

            var bytesCopied = 0L
            val buffer = ByteArray(64 * 1024) // 64KB buffer

            sourceInfo.stream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        coroutineContext.ensureActive()
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        onProgress?.invoke(bytesCopied, sourceInfo.size)
                    }
                    output.flush()
                }
            }

            // 4. Verify integrity
            if (sourceInfo.size > 0 && tempFile.length() != sourceInfo.size) {
                tempFile.delete()
                return@withContext ResultWrapper.Error(IllegalStateException("Copy integrity check failed: expected ${sourceInfo.size} bytes, got ${tempFile.length()}"))
            }

            // 5. Atomic commit: replace or rename
            if (finalTargetFile.exists()) {
                finalTargetFile.delete()
            }
            if (!tempFile.renameTo(finalTargetFile)) {
                tempFile.delete()
                return@withContext ResultWrapper.Error(IllegalStateException("Failed to commit copied file to destination: $finalTargetName"))
            }

            val details = getFileDetails(finalTargetFile.absolutePath)
            if (details is ResultWrapper.Success) {
                _storageEvents.tryEmit(StorageEvent.Created(details.data, source = "STORAGE"))
            }
            details
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun moveFile(
        sourcePathOrUri: String,
        targetParentPathOrUri: String,
        targetName: String?,
        resolution: FileConflictResolution
    ): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            val isDirectSource = !sourcePathOrUri.startsWith("content://")
            val isDirectTarget = !targetParentPathOrUri.startsWith("content://")

            // Attempt fast atomic move if both are standard files on the same filesystem
            if (isDirectSource && isDirectTarget) {
                val sourceFile = File(sourcePathOrUri)
                val targetParent = File(targetParentPathOrUri)
                val resolvedName = targetName ?: sourceFile.name
                val finalName = resolveTargetName(targetParentPathOrUri, resolvedName, resolution)
                    ?: return@withContext getFileDetails(File(targetParent, resolvedName).absolutePath)

                val targetFile = File(targetParent, finalName)

                if (targetFile.exists() && resolution == FileConflictResolution.OVERWRITE) {
                    targetFile.delete()
                }

                if (sourceFile.renameTo(targetFile)) {
                    val details = getFileDetails(targetFile.absolutePath)
                    if (details is ResultWrapper.Success) {
                        _storageEvents.tryEmit(StorageEvent.Moved(sourcePathOrUri, details.data))
                    }
                    return@withContext details
                }
            }

            // Fallback: Safe Copy -> Verify -> Delete source only upon verification
            val copyResult = copyFile(sourcePathOrUri, targetParentPathOrUri, targetName, resolution)
            if (copyResult is ResultWrapper.Success) {
                // Delete source safely
                deleteFile(sourcePathOrUri)
                _storageEvents.tryEmit(StorageEvent.Moved(sourcePathOrUri, copyResult.data))
            }
            copyResult
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun deleteFile(pathOrUri: String): ResultWrapper<Boolean> = withContext(dispatchers.io) {
        try {
            if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                val success = safManager.deleteDocument(uri)
                if (success) {
                    _storageEvents.tryEmit(StorageEvent.Deleted(pathOrUri))
                }
                ResultWrapper.Success(success)
            } else {
                val file = File(pathOrUri)
                if (!file.exists()) {
                    return@withContext ResultWrapper.Error(IllegalArgumentException("File not found: $pathOrUri"))
                }
                val success = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                if (success) {
                    _storageEvents.tryEmit(StorageEvent.Deleted(pathOrUri))
                }
                ResultWrapper.Success(success)
            }
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun renameFile(pathOrUri: String, newName: String): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            if (pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(pathOrUri)
                val renamedUri = safManager.renameDocument(uri, newName)
                    ?: return@withContext ResultWrapper.Error(IllegalStateException("Failed to rename SAF document to: $newName"))

                val details = getFileDetails(renamedUri.toString())
                if (details is ResultWrapper.Success) {
                    _storageEvents.tryEmit(StorageEvent.Moved(pathOrUri, details.data))
                }
                details
            } else {
                val file = File(pathOrUri)
                if (!file.exists()) {
                    return@withContext ResultWrapper.Error(IllegalArgumentException("File not found: $pathOrUri"))
                }
                val target = File(file.parentFile, newName)
                if (target.exists()) {
                    return@withContext ResultWrapper.Error(IllegalStateException("File with target name already exists: $newName"))
                }
                if (!file.renameTo(target)) {
                    return@withContext ResultWrapper.Error(IllegalStateException("Failed to rename file to: $newName"))
                }

                val details = getFileDetails(target.absolutePath)
                if (details is ResultWrapper.Success) {
                    _storageEvents.tryEmit(StorageEvent.Moved(pathOrUri, details.data))
                }
                details
            }
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun takePersistableUriPermission(uri: Uri): ResultWrapper<Boolean> = withContext(dispatchers.io) {
        val success = safManager.takePersistablePermission(uri)
        ResultWrapper.Success(success)
    }

    override suspend fun releasePersistableUriPermission(uri: Uri): ResultWrapper<Boolean> = withContext(dispatchers.io) {
        val success = safManager.releasePersistablePermission(uri)
        ResultWrapper.Success(success)
    }

    override suspend fun getPersistedPermissions(): ResultWrapper<List<SafPermission>> = withContext(dispatchers.io) {
        val perms = safManager.getPersistedPermissions()
        ResultWrapper.Success(perms)
    }

    override suspend fun isUriPermissionGranted(uri: Uri): Boolean = withContext(dispatchers.io) {
        safManager.isPermissionGranted(uri)
    }

    private data class SourceInfo(val stream: InputStream, val size: Long, val name: String)

    private fun openSource(pathOrUri: String): SourceInfo? {
        return if (pathOrUri.startsWith("content://")) {
            val uri = Uri.parse(pathOrUri)
            val stream = safManager.openInputStream(uri) ?: return null
            var name = "file"
            var size = -1L

            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameCol >= 0) name = cursor.getString(nameCol) ?: name
                    if (sizeCol >= 0) size = cursor.getLong(sizeCol)
                }
            }

            SourceInfo(stream, size, name)
        } else {
            val file = File(pathOrUri)
            if (!file.exists() || !file.canRead()) return null
            SourceInfo(FileInputStream(file), file.length(), file.name)
        }
    }

    private fun resolveTargetName(parentPath: String, name: String, resolution: FileConflictResolution): String? {
        val parent = File(parentPath)
        val candidate = File(parent, name)

        if (!candidate.exists()) {
            return name
        }

        when (resolution) {
            FileConflictResolution.OVERWRITE -> return name
            FileConflictResolution.SKIP -> return null
            FileConflictResolution.AUTO_RENAME -> {
                val ext = FileExtensions.getExtension(name)
                val base = name.substringBeforeLast('.', name)
                var index = 1
                while (index < 1000) {
                    val candidateName = if (ext.isNotEmpty()) "$base ($index).$ext" else "$base ($index)"
                    if (!File(parent, candidateName).exists()) {
                        return candidateName
                    }
                    index++
                }
                return "$base (${System.currentTimeMillis()}).$ext"
            }
        }
    }

    private fun sortFileItems(items: List<FileItem>, sortOption: FileSortOption): List<FileItem> {
        return when (sortOption) {
            FileSortOption.NAME_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            FileSortOption.NAME_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
            FileSortOption.DATE_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
            FileSortOption.DATE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            FileSortOption.SIZE_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            FileSortOption.SIZE_DESC -> items.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
            FileSortOption.TYPE_ASC -> items.sortedWith(compareBy({ !it.isDirectory }, { it.extension }, { it.name.lowercase() }))
        }
    }
}
