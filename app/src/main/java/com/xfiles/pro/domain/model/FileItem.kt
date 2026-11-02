package com.xfiles.pro.domain.model

/**
 * Core domain representation of a file system item.
 */
data class FileItem(
    val name: String,
    val path: String,
    val uriString: String? = null,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val fileType: FileType = FileType.UNKNOWN,
    val extension: String = "",
    val isHidden: Boolean = false,
    val canRead: Boolean = true,
    val canWrite: Boolean = true
)
