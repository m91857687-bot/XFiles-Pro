package com.xfiles.pro.data.repository

import android.content.Context
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.database.dao.DownloadTaskDao
import com.xfiles.pro.data.local.database.entity.DownloadTaskEntity
import com.xfiles.pro.data.local.download.DownloadFileNameExtractor
import com.xfiles.pro.data.local.download.DownloadQueueManager
import com.xfiles.pro.data.local.download.service.DownloadForegroundService
import com.xfiles.pro.data.local.download.service.DownloadNotificationHelper
import com.xfiles.pro.domain.model.DownloadItem
import com.xfiles.pro.domain.model.DownloadRequest
import com.xfiles.pro.domain.model.DownloadStatus
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.repository.DownloadRepository
import com.xfiles.pro.domain.repository.FileIndexingRepository
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Implementation of DownloadRepository managing download lifecycle, Room persistence,
 * filesystem updates, and notifications.
 */
class DownloadRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatchersProvider,
    private val downloadTaskDao: DownloadTaskDao,
    private val queueManager: DownloadQueueManager,
    private val storageRepository: StorageRepository? = null,
    private val fileIndexingRepository: FileIndexingRepository? = null,
    private val notificationHelper: DownloadNotificationHelper = DownloadNotificationHelper(context)
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadItem>> {
        return combine(
            downloadTaskDao.getAllTasksFlow(),
            queueManager.telemetryMap
        ) { entities, telemetryMap ->
            entities.map { entity ->
                val telemetry = telemetryMap[entity.id]
                entity.toDomain(
                    downloadSpeed = telemetry?.speedBytesPerSec ?: 0L,
                    etaSeconds = telemetry?.etaSeconds ?: -1L
                )
            }
        }
    }

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> {
        return combine(
            downloadTaskDao.getTasksByStatusFlow(status.name),
            queueManager.telemetryMap
        ) { entities, telemetryMap ->
            entities.map { entity ->
                val telemetry = telemetryMap[entity.id]
                entity.toDomain(
                    downloadSpeed = telemetry?.speedBytesPerSec ?: 0L,
                    etaSeconds = telemetry?.etaSeconds ?: -1L
                )
            }
        }
    }

    override fun getDownloadById(id: String): Flow<DownloadItem?> {
        return combine(
            downloadTaskDao.getTaskByIdFlow(id),
            queueManager.telemetryMap
        ) { entity, telemetryMap ->
            entity?.let {
                val telemetry = telemetryMap[it.id]
                it.toDomain(
                    downloadSpeed = telemetry?.speedBytesPerSec ?: 0L,
                    etaSeconds = telemetry?.etaSeconds ?: -1L
                )
            }
        }
    }

    override suspend fun enqueueDownload(request: DownloadRequest): ResultWrapper<String> = withContext(dispatchers.io) {
        try {
            val destDir = File(request.destinationDirectory)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            val extractedName = DownloadFileNameExtractor.extractFileName(
                url = request.url,
                suggestedName = request.fileName
            )
            val uniqueName = DownloadFileNameExtractor.resolveUniqueFileName(destDir, extractedName)
            val finalPath = File(destDir, uniqueName).absolutePath

            val taskId = UUID.randomUUID().toString()
            val entity = DownloadTaskEntity(
                id = taskId,
                url = request.url,
                fileName = uniqueName,
                destinationDirectory = request.destinationDirectory,
                destinationFilePath = finalPath,
                totalBytes = -1L,
                downloadedBytes = 0L,
                status = DownloadStatus.QUEUED.name,
                mimeType = null,
                eTag = null,
                lastModified = null,
                isResumable = true,
                retryCount = 0,
                errorMessage = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            downloadTaskDao.insertTask(entity)
            queueManager.enqueue(taskId)

            try {
                DownloadForegroundService.startService(context)
            } catch (_: Exception) {
                // In unit testing environment, service start may be bypassed
            }

            ResultWrapper.Success(taskId)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun pauseDownload(id: String): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            queueManager.pause(id)
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun resumeDownload(id: String): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            queueManager.enqueue(id)
            try {
                DownloadForegroundService.startService(context)
            } catch (_: Exception) {}
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun cancelDownload(id: String): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            queueManager.cancel(id)
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun retryDownload(id: String): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            val task = downloadTaskDao.getTaskById(id)
                ?: return@withContext ResultWrapper.Error(IllegalArgumentException("Task not found"))

            // Reset downloaded bytes if it failed completely or part file missing
            val partFile = File(task.destinationDirectory, "${task.fileName}.part")
            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            val updated = task.copy(
                downloadedBytes = existingBytes,
                status = DownloadStatus.QUEUED.name,
                retryCount = task.retryCount + 1,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            downloadTaskDao.updateTask(updated)
            queueManager.enqueue(id)

            try {
                DownloadForegroundService.startService(context)
            } catch (_: Exception) {}

            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun deleteDownload(id: String, deleteFile: Boolean): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            val task = downloadTaskDao.getTaskById(id)
            if (task != null) {
                queueManager.cancel(id)

                if (deleteFile) {
                    val file = File(task.destinationFilePath)
                    if (file.exists()) {
                        file.delete()
                        fileIndexingRepository?.onFileDeleted(task.destinationFilePath)
                    }
                    val partFile = File(task.destinationDirectory, "${task.fileName}.part")
                    if (partFile.exists()) {
                        partFile.delete()
                    }
                }
                downloadTaskDao.deleteTaskById(id)
            }
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun renameCompletedDownload(id: String, newName: String): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            val task = downloadTaskDao.getTaskById(id)
                ?: return@withContext ResultWrapper.Error(IllegalArgumentException("Task not found"))

            val sanitizedNewName = DownloadFileNameExtractor.sanitize(newName)
            if (sanitizedNewName.isBlank()) {
                return@withContext ResultWrapper.Error(IllegalArgumentException("Invalid file name"))
            }

            val dir = File(task.destinationDirectory)
            val currentFile = File(task.destinationFilePath)
            val targetFile = File(dir, sanitizedNewName)

            if (targetFile.exists() && targetFile.canonicalPath != currentFile.canonicalPath) {
                return@withContext ResultWrapper.Error(IOException("A file with this name already exists"))
            }

            if (currentFile.exists()) {
                val renamed = currentFile.renameTo(targetFile)
                if (!renamed) {
                    currentFile.copyTo(targetFile, overwrite = true)
                    currentFile.delete()
                }
                fileIndexingRepository?.onFileMoved(
                    oldPath = currentFile.absolutePath,
                    newItem = FileItem(
                        name = targetFile.name,
                        path = targetFile.absolutePath,
                        size = targetFile.length(),
                        lastModified = targetFile.lastModified(),
                        isDirectory = false
                    )
                )
            }

            downloadTaskDao.updateFileNameAndPath(
                id = id,
                fileName = sanitizedNewName,
                destinationFilePath = targetFile.absolutePath,
                updatedAt = System.currentTimeMillis()
            )

            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override suspend fun clearCompletedDownloads(): ResultWrapper<Unit> = withContext(dispatchers.io) {
        try {
            downloadTaskDao.deleteAllCompleted()
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e)
        }
    }

    override fun getActiveDownloadsCount(): Flow<Int> {
        return downloadTaskDao.getActiveDownloadsCountFlow()
    }

    /**
     * Called by DownloadQueueManager when a download succeeds.
     * Integrates with file indexing and shows completion notification.
     */
    fun onDownloadFinished(task: DownloadTaskEntity, finalFile: File) {
        // Trigger incremental indexing for file manager integration
        fileIndexingRepository?.let { indexing ->
            CoroutineScope(dispatchers.io).launch {
                try {
                    indexing.onFileCreatedOrUpdated(
                        item = FileItem(
                            name = finalFile.name,
                            path = finalFile.absolutePath,
                            size = finalFile.length(),
                            lastModified = finalFile.lastModified(),
                            isDirectory = false
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        // Show completion notification
        try {
            notificationHelper.showCompletedNotification(task.toDomain())
        } catch (_: Exception) {}
    }
}
