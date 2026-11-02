package com.xfiles.pro.domain.model

/**
 * Domain model representing an accessible storage volume (Internal, SD Card, USB OTG).
 */
data class StorageVolume(
    val id: String,
    val name: String,
    val path: String,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val isPrimary: Boolean = false,
    val isRemovable: Boolean = false
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
}
