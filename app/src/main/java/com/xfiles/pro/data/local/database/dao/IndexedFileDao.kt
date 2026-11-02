package com.xfiles.pro.data.local.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Lightweight tuple for fast incremental change detection without reading full records.
 */
data class FileMetadataTuple(
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "last_modified") val lastModified: Long,
    @ColumnInfo(name = "size") val size: Long
)

/**
 * Data Access Object for indexed file operations, fast SQLite FTS full-text search,
 * and high-efficiency incremental sync operations.
 */
@Dao
interface IndexedFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: IndexedFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<IndexedFileEntity>)

    @Update
    suspend fun update(entity: IndexedFileEntity)

    @Query("DELETE FROM indexed_files WHERE path = :path")
    suspend fun deleteByPath(path: String): Int

    @Query("DELETE FROM indexed_files WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>): Int

    @Query("DELETE FROM indexed_files WHERE parent_path = :parentPath")
    suspend fun deleteByParentPath(parentPath: String): Int

    @Query("SELECT * FROM indexed_files WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): IndexedFileEntity?

    @Query("SELECT * FROM indexed_files WHERE name = :name AND size = :size LIMIT 1")
    suspend fun findByNameAndSize(name: String, size: Long): IndexedFileEntity?

    @Query("SELECT * FROM indexed_files WHERE parent_path = :parentPath ORDER BY is_directory DESC, name ASC")
    fun getByParentPath(parentPath: String): Flow<List<IndexedFileEntity>>

    @Query("SELECT * FROM indexed_files WHERE category = :category ORDER BY is_directory DESC, name ASC")
    fun getByCategory(category: String): Flow<List<IndexedFileEntity>>

    @Query("SELECT * FROM indexed_files WHERE extension = :extension ORDER BY is_directory DESC, name ASC")
    fun getByExtension(extension: String): Flow<List<IndexedFileEntity>>

    @Query("SELECT * FROM indexed_files WHERE volume_id = :volumeId")
    suspend fun getAllByVolume(volumeId: String): List<IndexedFileEntity>

    @Query("SELECT path, last_modified, size FROM indexed_files WHERE volume_id = :volumeId")
    suspend fun getMetadataByVolume(volumeId: String): List<FileMetadataTuple>

    @Query("SELECT path FROM indexed_files")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT COUNT(*) FROM indexed_files")
    suspend fun getCount(): Long

    @Query("SELECT COUNT(*) FROM indexed_files")
    fun getCountFlow(): Flow<Long>

    @Query("DELETE FROM indexed_files WHERE volume_id = :volumeId")
    suspend fun deleteByVolume(volumeId: String): Int

    @Query("DELETE FROM indexed_files")
    suspend fun clearAll()

    /**
     * SQLite FTS4 full text search matched against virtual table.
     */
    @Query("""
        SELECT f.* FROM indexed_files f
        JOIN indexed_files_fts fts ON f.id = fts.rowid
        WHERE indexed_files_fts MATCH :matchQuery
        ORDER BY f.is_directory DESC, f.name ASC
        LIMIT 200
    """)
    fun searchFts(matchQuery: String): Flow<List<IndexedFileEntity>>

    /**
     * FTS search with category filter.
     */
    @Query("""
        SELECT f.* FROM indexed_files f
        JOIN indexed_files_fts fts ON f.id = fts.rowid
        WHERE indexed_files_fts MATCH :matchQuery AND f.category = :category
        ORDER BY f.is_directory DESC, f.name ASC
        LIMIT 200
    """)
    fun searchFtsWithCategory(matchQuery: String, category: String): Flow<List<IndexedFileEntity>>

    /**
     * Substring LIKE search fallback for partial and middle-of-word matching across Arabic and English.
     */
    @Query("""
        SELECT * FROM indexed_files
        WHERE name LIKE '%' || :query || '%'
           OR extension LIKE '%' || :query || '%'
        ORDER BY is_directory DESC, name ASC
        LIMIT 200
    """)
    fun searchByNameLike(query: String): Flow<List<IndexedFileEntity>>

    /**
     * Atomic batch update for incremental synchronization:
     * - Deletes removed files
     * - Inserts new or modified files
     */
    @Transaction
    suspend fun syncBatch(toDeletePaths: List<String>, toInsert: List<IndexedFileEntity>) {
        if (toDeletePaths.isNotEmpty()) {
            deleteByPaths(toDeletePaths)
        }
        if (toInsert.isNotEmpty()) {
            insertAll(toInsert)
        }
    }
}
