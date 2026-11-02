package com.xfiles.pro.domain.model

import java.util.UUID

/**
 * Represents an individual browsing tab in multi-tab file exploration.
 * Retains independent path, view mode (list/grid), and sort preference.
 */
data class FileTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val path: String,
    val isGridView: Boolean = false,
    val sortOption: FileSortOption = FileSortOption.NAME_ASC,
    val createdAt: Long = System.currentTimeMillis()
)
