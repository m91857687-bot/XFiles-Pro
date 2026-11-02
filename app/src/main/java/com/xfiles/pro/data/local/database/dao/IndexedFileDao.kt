package com.xfiles.pro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for indexed file operations.
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

    @Query("DELETE FROM indexed_files WHERE parent_path = :parentPath")
    suspend fun deleteByParentPath(parentPath: String): Int

    @Query("SELECT * FROM indexed_files WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): IndexedFileEntity?

    @Query("SELECT * FROM indexed_files WHERE parent_path = :parentPath ORDER BY is_directory DESC, name ASC")
    fun getByParentPath(parentPath: String): Flow<List<IndexedFileEntity>>

    @Query("SELECT * FROM indexed_files WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 100")
    fun searchByName(query: String): Flow<List<IndexedFileEntity>>

    @Query("SELECT COUNT(*) FROM indexed_files")
    suspend fun getCount(): Long

    @Query("DELETE FROM indexed_files")
    suspend fun clearAll()
}
