package com.xfiles.pro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing an indexed file entry for persistent indexing.
 */
@Entity(
    tableName = "indexed_files",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["parent_path"]),
        Index(value = ["category"]),
        Index(value = ["extension"]),
        Index(value = ["last_modified"])
    ]
)
data class IndexedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "parent_path")
    val parentPath: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "is_directory")
    val isDirectory: Boolean,

    @ColumnInfo(name = "extension")
    val extension: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "category")
    val category: String
)
