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
        Index(value = ["last_modified"]),
        Index(value = ["volume_id"]),
        Index(value = ["source"])
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
    val category: String,

    @ColumnInfo(name = "source", defaultValue = "STORAGE")
    val source: String = "STORAGE", // "SAF", "MEDIA_STORE", "STORAGE"

    @ColumnInfo(name = "volume_id", defaultValue = "primary")
    val volumeId: String = "primary",

    @ColumnInfo(name = "can_read", defaultValue = "1")
    val canRead: Boolean = true,

    @ColumnInfo(name = "can_write", defaultValue = "1")
    val canWrite: Boolean = true,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = 0L,

    @ColumnInfo(name = "last_indexed_at", defaultValue = "0")
    val lastIndexedAt: Long = System.currentTimeMillis()
)
