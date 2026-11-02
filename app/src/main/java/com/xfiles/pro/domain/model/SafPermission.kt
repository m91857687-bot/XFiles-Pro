package com.xfiles.pro.domain.model

/**
 * Represents a persisted Storage Access Framework (SAF) permission.
 */
data class SafPermission(
    val uriString: String,
    val isReadGranted: Boolean = true,
    val isWriteGranted: Boolean = true,
    val persistedTime: Long = System.currentTimeMillis()
)
