package com.xfiles.pro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * SQLite FTS4 virtual table for high-speed full-text search across indexed files.
 * Backed directly by [IndexedFileEntity] for automatic synchronization.
 * Uses unicode61 tokenizer for comprehensive international and Arabic language support.
 */
@Entity(tableName = "indexed_files_fts")
@Fts4(contentEntity = IndexedFileEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class IndexedFileFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "extension")
    val extension: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "parent_path")
    val parentPath: String
)
