package com.xfiles.pro.domain.model

/**
 * Represents a single file or directory entry inside an archive.
 */
data class ArchiveEntryItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val uncompressedSize: Long = 0L,
    val compressedSize: Long = 0L,
    val lastModified: Long = 0L,
    val isEncrypted: Boolean = false,
    val comment: String? = null
)
