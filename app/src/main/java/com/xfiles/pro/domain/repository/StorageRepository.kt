package com.xfiles.pro.domain.repository

import android.net.Uri
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.SafPermission
import com.xfiles.pro.domain.model.StorageVolume

/**
 * Domain contract for accessing storage volumes, managing files, and interfacing with SAF and MediaStore.
 */
interface StorageRepository {
    suspend fun getStorageVolumes(): ResultWrapper<List<StorageVolume>>
    suspend fun listFiles(pathOrUri: String, sortOption: FileSortOption = FileSortOption.NAME_ASC): ResultWrapper<List<FileItem>>
    suspend fun getFileDetails(pathOrUri: String): ResultWrapper<FileItem>

    // CRUD Operations
    suspend fun createDirectory(parentPathOrUri: String, name: String): ResultWrapper<FileItem>
    suspend fun createFile(parentPathOrUri: String, name: String, mimeType: String = "text/plain"): ResultWrapper<FileItem>
    suspend fun copyFile(
        sourcePathOrUri: String,
        targetParentPathOrUri: String,
        targetName: String? = null,
        resolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        onProgress: ((bytesCopied: Long, totalBytes: Long) -> Unit)? = null
    ): ResultWrapper<FileItem>
    suspend fun moveFile(
        sourcePathOrUri: String,
        targetParentPathOrUri: String,
        targetName: String? = null,
        resolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME
    ): ResultWrapper<FileItem>
    suspend fun deleteFile(pathOrUri: String): ResultWrapper<Boolean>
    suspend fun renameFile(pathOrUri: String, newName: String): ResultWrapper<FileItem>

    // SAF Permissions
    suspend fun takePersistableUriPermission(uri: Uri): ResultWrapper<Boolean>
    suspend fun releasePersistableUriPermission(uri: Uri): ResultWrapper<Boolean>
    suspend fun getPersistedPermissions(): ResultWrapper<List<SafPermission>>
    suspend fun isUriPermissionGranted(uri: Uri): Boolean
    // Storage Events for real-time indexing sync
    val storageEvents: kotlinx.coroutines.flow.Flow<com.xfiles.pro.domain.model.StorageEvent>
}
