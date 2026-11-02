package com.xfiles.pro.domain.model

/**
 * State representation for persistent file indexing operations.
 */
data class IndexingState(
    val isIndexing: Boolean = false,
    val indexedCount: Long = 0L,
    val totalEstimated: Long = 0L,
    val currentVolume: String? = null,
    val progress: Float = 0f,
    val error: String? = null
)
