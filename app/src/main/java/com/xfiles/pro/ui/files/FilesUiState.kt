package com.xfiles.pro.ui.files

import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileTab
import com.xfiles.pro.domain.model.IndexingState
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.domain.model.SearchFilterCriteria

/**
 * Complete UI State for FilesScreen supporting:
 * - Multi-Tab browsing with persistent state
 * - Dual Pane layout with cross-pane transfers
 * - Advanced multi-filter search with history
 * - In-place CRUD and index synchronization
 */
data class FilesUiState(
    // Multi-Tab State
    val tabs: List<FileTab> = emptyList(),
    val activeTabIndex: Int = 0,

    // Dual Pane State
    val isDualPaneMode: Boolean = false,
    val activePaneId: PaneId = PaneId.PRIMARY,
    val primaryPane: PaneState = PaneState(),
    val secondaryPane: PaneState = PaneState(),

    // Advanced Search State
    val searchCriteria: SearchFilterCriteria = SearchFilterCriteria(),
    val isSearching: Boolean = false,
    val searchResults: List<FileItem> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val showFilterSheet: Boolean = false,
    val isSearchHistoryVisible: Boolean = false,

    // Indexing status
    val indexingState: IndexingState = IndexingState(),
    val totalIndexedCount: Long = 0L,

    // Cross-pane operations & progress
    val isTransferring: Boolean = false,
    val transferProgress: Float = 0f,
    val transferMessage: String? = null,

    // Dialogs & Feedback
    val showCreateFolderDialog: Boolean = false,
    val showCreateFileDialog: Boolean = false,
    val itemToRename: FileItem? = null,
    val itemToDelete: FileItem? = null,
    val operationSuccessMessage: String? = null,
    val operationErrorMessage: String? = null
) {
    val activePaneState: PaneState
        get() = if (activePaneId == PaneId.PRIMARY) primaryPane else secondaryPane

    val targetPaneState: PaneState
        get() = if (activePaneId == PaneId.PRIMARY) secondaryPane else primaryPane

    val activeTab: FileTab?
        get() = tabs.getOrNull(activeTabIndex)

    // Backward-compatible delegates for existing usages
    val currentPath: String
        get() = activePaneState.currentPath

    val breadcrumbs: List<Pair<String, String>>
        get() = activePaneState.breadcrumbs

    val files: List<FileItem>
        get() = activePaneState.files

    val isLoading: Boolean
        get() = activePaneState.isLoading

    val errorMessage: String?
        get() = operationErrorMessage ?: activePaneState.errorMessage

    val sortOption: FileSortOption
        get() = activePaneState.sortOption

    val isGridView: Boolean
        get() = activePaneState.isGridView

    val selectedItemForDetails: FileItem?
        get() = activePaneState.selectedItemForDetails

    val searchQuery: String
        get() = searchCriteria.query
}
