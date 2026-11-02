package com.xfiles.pro.data.repository

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.StorageVolume
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.withContext
import java.io.File

/**
 * StorageRepository implementation utilizing File APIs and StorageManager.
 */
class StorageRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatchersProvider
) : StorageRepository {

    override suspend fun getStorageVolumes(): ResultWrapper<List<StorageVolume>> = withContext(dispatchers.io) {
        try {
            val volumeList = mutableListOf<StorageVolume>()
            val primaryDir = Environment.getExternalStorageDirectory()
            val totalBytes = primaryDir.totalSpace
            val freeBytes = primaryDir.freeSpace

            volumeList.add(
                StorageVolume(
                    id = "primary",
                    name = "Internal Storage",
                    path = primaryDir.absolutePath,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    isPrimary = true,
                    isRemovable = false
                )
            )

            // Query additional mounted volumes if available
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (storageManager != null) {
                val volumes = storageManager.storageVolumes
                for (vol in volumes) {
                    if (vol.isRemovable) {
                        val desc = vol.getDescription(context)
                        val dir = vol.directory
                        if (dir != null && dir.exists()) {
                            volumeList.add(
                                StorageVolume(
                                    id = vol.uuid ?: dir.name,
                                    name = desc ?: "SD Card",
                                    path = dir.absolutePath,
                                    totalBytes = dir.totalSpace,
                                    freeBytes = dir.freeSpace,
                                    isPrimary = false,
                                    isRemovable = true
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

    override suspend fun listFiles(path: String, sortOption: FileSortOption): ResultWrapper<List<FileItem>> = withContext(dispatchers.io) {
        try {
            val directory = File(path)
            if (!directory.exists()) {
                return@withContext ResultWrapper.Error(IllegalArgumentException("Path does not exist: $path"))
            }
            if (!directory.isDirectory) {
                return@withContext ResultWrapper.Error(IllegalArgumentException("Path is not a directory: $path"))
            }

            val rawFiles = directory.listFiles() ?: emptyArray()
            val fileItems = rawFiles.map { file ->
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

            val sortedItems = when (sortOption) {
                FileSortOption.NAME_ASC -> fileItems.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                FileSortOption.NAME_DESC -> fileItems.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() })
                FileSortOption.DATE_ASC -> fileItems.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
                FileSortOption.DATE_DESC -> fileItems.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
                FileSortOption.SIZE_ASC -> fileItems.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
                FileSortOption.SIZE_DESC -> fileItems.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size })
                FileSortOption.TYPE_ASC -> fileItems.sortedWith(compareBy({ !it.isDirectory }, { it.extension }, { it.name.lowercase() }))
            }

            ResultWrapper.Success(sortedItems)
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }

    override suspend fun getFileDetails(path: String): ResultWrapper<FileItem> = withContext(dispatchers.io) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext ResultWrapper.Error(IllegalArgumentException("File does not exist: $path"))
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
        } catch (t: Throwable) {
            ResultWrapper.Error(t)
        }
    }
}
