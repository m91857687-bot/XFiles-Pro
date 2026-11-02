package com.xfiles.pro.domain.repository

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.StorageVolume

/**
 * Domain contract for accessing storage volumes and listing files.
 */
interface StorageRepository {
    suspend fun getStorageVolumes(): ResultWrapper<List<StorageVolume>>
    suspend fun listFiles(path: String, sortOption: FileSortOption = FileSortOption.NAME_ASC): ResultWrapper<List<FileItem>>
    suspend fun getFileDetails(path: String): ResultWrapper<FileItem>
}
