package com.xfiles.pro.domain.repository

import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.IndexingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain contract for high-performance persistent file indexing, fast search,
 * and real-time synchronization with storage changes.
 */
interface FileIndexingRepository {

    /**
     * Observable state of the indexing engine (running, idle, progress, counts).
     */
    val indexingState: StateFlow<IndexingState>

    /**
     * Real-time count of currently indexed files in the persistent database.
     */
    fun getIndexedFilesCount(): Flow<Long>

    /**
     * Search files using SQLite FTS with progressive prefix matching,
     * category filtering, and fallback substring matching.
     */
    fun searchFiles(query: String, fileType: FileType? = null): Flow<List<FileItem>>

    /**
     * Retrieve files by parent directory path directly from the index.
     */
    fun getFilesByParent(parentPath: String): Flow<List<FileItem>>

    /**
     * Run full or incremental scan across all available storage volumes and MediaStore.
     */
    suspend fun runIndexing(forceRescan: Boolean = false): Result<Int>

    /**
     * Cancel ongoing indexing work.
     */
    fun cancelIndexing()

    /**
     * Synchronously insert or update a file after a storage CRUD operation.
     */
    suspend fun onFileCreatedOrUpdated(item: FileItem, source: String = "STORAGE", volumeId: String = "primary")

    /**
     * Synchronously delete a file from the index after deletion.
     */
    suspend fun onFileDeleted(path: String)

    /**
     * Synchronously handle file moves / renames in the index.
     */
    suspend fun onFileMoved(oldPath: String, newItem: FileItem)

    /**
     * Purge all index records or records for a specific volume.
     */
    suspend fun clearIndex(volumeId: String? = null)

    /**
     * Enqueue background periodic / initial indexing using WorkManager.
     */
    fun scheduleBackgroundIndexing()
}
