package com.xfiles.pro.domain.model

/**
 * Real-time progress state for archive operations (creation, extraction, modification).
 */
data class ArchiveProgress(
    val currentItem: String = "",
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Float = 0f,
    val isIndeterminate: Boolean = false
)
