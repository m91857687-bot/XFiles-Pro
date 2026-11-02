package com.xfiles.pro.ui.files

import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.IndexingState

/**
 * UI State for the FilesScreen managing navigation, browsing, file operations,
 * full-text search, and persistent indexing status.
 */
data class FilesUiState(
    val currentPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(), // List of (displayName, fullPath)
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sortOption: FileSortOption = FileSortOption.NAME_ASC,
    val isGridView: Boolean = false,
    val selectedItemForDetails: FileItem? = null,
    val showCreateFolderDialog: Boolean = false,
    val showCreateFileDialog: Boolean = false,
    val itemToRename: FileItem? = null,
    val itemToDelete: FileItem? = null,
    val operationSuccessMessage: String? = null,

    // Phase 5B: Instant search & indexing engine states
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<FileItem> = emptyList(),
    val indexingState: IndexingState = IndexingState(),
    val totalIndexedCount: Long = 0L
)
