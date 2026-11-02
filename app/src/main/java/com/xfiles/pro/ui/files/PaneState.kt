package com.xfiles.pro.ui.files

import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption

/**
 * State representing an individual browsing panel in single or dual pane exploration.
 */
data class PaneState(
    val currentPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sortOption: FileSortOption = FileSortOption.NAME_ASC,
    val isGridView: Boolean = false,
    val selectedItemForDetails: FileItem? = null,
    val selectedItemForAction: FileItem? = null
)
