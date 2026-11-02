package com.xfiles.pro.ui.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.DateFilterRange
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileTab
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.domain.model.SearchFilterCriteria
import com.xfiles.pro.domain.model.SearchTarget
import com.xfiles.pro.domain.model.SizeFilterRange
import com.xfiles.pro.domain.repository.FileIndexingRepository
import com.xfiles.pro.domain.repository.SearchHistoryRepository
import com.xfiles.pro.domain.repository.StorageRepository
import com.xfiles.pro.domain.repository.TabRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel managing Phase 6 features:
 * - Multi-Tab persistent file exploration
 * - Dual Pane side-by-side browsing & cross-pane operations
 * - Advanced search with multi-filters and persistent history
 * - Safe CRUD operations and real-time index synchronization
 */
class FilesViewModel(
    private val storageRepository: StorageRepository,
    private val fileIndexingRepository: FileIndexingRepository,
    private val tabRepository: TabRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val archiveRepository: com.xfiles.pro.domain.repository.ArchiveRepository? = null
) : ViewModel() {

    val rootPath: String by lazy {
        Environment.getExternalStorageDirectory().absolutePath
    }

    private val _uiState = MutableStateFlow(
        FilesUiState(
            tabs = listOf(FileTab(title = "Storage", path = Environment.getExternalStorageDirectory().absolutePath)),
            activeTabIndex = 0,
            primaryPane = PaneState(currentPath = Environment.getExternalStorageDirectory().absolutePath),
            secondaryPane = PaneState(currentPath = Environment.getExternalStorageDirectory().absolutePath)
        )
    )
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Initialize Multi-Tabs from persistence or fallback to default root
        viewModelScope.launch {
            val savedTabs = tabRepository.savedTabs.firstOrNull() ?: emptyList()
            val savedIndex = tabRepository.activeTabIndex.firstOrNull() ?: 0

            if (savedTabs.isNotEmpty()) {
                // Verify each tab path; if inaccessible, fallback safely to rootPath
                val validatedTabs = savedTabs.map { tab ->
                    val file = File(tab.path)
                    if (file.exists() && file.canRead()) tab else tab.copy(path = rootPath, title = "Storage")
                }
                val validIndex = savedIndex.coerceIn(0, validatedTabs.lastIndex)
                val activeTab = validatedTabs[validIndex]

                _uiState.update { current ->
                    if (current.tabs.size <= 1) {
                        current.copy(
                            tabs = validatedTabs,
                            activeTabIndex = validIndex,
                            primaryPane = current.primaryPane.copy(
                                currentPath = activeTab.path,
                                isGridView = activeTab.isGridView,
                                sortOption = activeTab.sortOption
                            ),
                            secondaryPane = current.secondaryPane.copy(
                                currentPath = rootPath
                            )
                        )
                    } else {
                        current
                    }
                }

                loadDirectory(activeTab.path, PaneId.PRIMARY)
                loadDirectory(rootPath, PaneId.SECONDARY)
            } else {
                loadDirectory(rootPath, PaneId.PRIMARY)
                loadDirectory(rootPath, PaneId.SECONDARY)
            }
        }

        // Observe search history
        viewModelScope.launch {
            searchHistoryRepository.recentSearches.collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }

        // Observe persistent indexing engine states
        viewModelScope.launch {
            fileIndexingRepository.indexingState.collect { indexingState ->
                _uiState.update { it.copy(indexingState = indexingState) }
            }
        }

        viewModelScope.launch {
            fileIndexingRepository.getIndexedFilesCount().collect { count ->
                _uiState.update { it.copy(totalIndexedCount = count) }
            }
        }
    }

    // ==========================================
    // MULTI-TAB MANAGEMENT
    // ==========================================

    fun openNewTab(path: String = rootPath, title: String? = null) {
        val resolvedTitle = title ?: getDirectoryDisplayName(path)
        val newTab = FileTab(
            title = resolvedTitle,
            path = path,
            isGridView = _uiState.value.activePaneState.isGridView,
            sortOption = _uiState.value.activePaneState.sortOption
        )
        val updatedTabs = _uiState.value.tabs + newTab
        val newIndex = updatedTabs.lastIndex

        _uiState.update {
            it.copy(
                tabs = updatedTabs,
                activeTabIndex = newIndex
            )
        }
        loadDirectory(path, _uiState.value.activePaneId)
        persistTabs()
    }

    fun openFolderInNewTab(item: FileItem) {
        if (item.isDirectory) {
            openNewTab(item.path, item.name)
        }
    }

    fun switchTab(index: Int) {
        val tabs = _uiState.value.tabs
        if (index in tabs.indices && index != _uiState.value.activeTabIndex) {
            val tab = tabs[index]
            _uiState.update {
                it.copy(
                    activeTabIndex = index,
                    primaryPane = it.primaryPane.copy(
                        isGridView = tab.isGridView,
                        sortOption = tab.sortOption
                    )
                )
            }
            loadDirectory(tab.path, _uiState.value.activePaneId)
            persistTabs()
        }
    }

    fun closeTab(index: Int) {
        val currentTabs = _uiState.value.tabs
        if (currentTabs.size <= 1) return // Do not close the last remaining tab

        val updatedTabs = currentTabs.toMutableList().apply { removeAt(index) }
        var newActiveIndex = _uiState.value.activeTabIndex

        if (index == _uiState.value.activeTabIndex) {
            newActiveIndex = (index - 1).coerceAtLeast(0)
        } else if (index < _uiState.value.activeTabIndex) {
            newActiveIndex = (_uiState.value.activeTabIndex - 1).coerceAtLeast(0)
        }

        val targetTab = updatedTabs[newActiveIndex]
        _uiState.update {
            it.copy(
                tabs = updatedTabs,
                activeTabIndex = newActiveIndex
            )
        }
        loadDirectory(targetTab.path, _uiState.value.activePaneId)
        persistTabs()
    }

    fun closeOtherTabs() {
        val active = _uiState.value.activeTab ?: return
        _uiState.update {
            it.copy(
                tabs = listOf(active),
                activeTabIndex = 0
            )
        }
        persistTabs()
    }

    private fun persistTabs() {
        val state = _uiState.value
        viewModelScope.launch {
            tabRepository.saveTabs(state.tabs, state.activeTabIndex)
        }
    }

    // ==========================================
    // DUAL PANE MANAGEMENT
    // ==========================================

    fun toggleDualPaneMode() {
        val newMode = !_uiState.value.isDualPaneMode
        setDualPaneMode(newMode)
    }

    fun setDualPaneMode(enabled: Boolean) {
        _uiState.update { it.copy(isDualPaneMode = enabled) }
        if (enabled && _uiState.value.secondaryPane.files.isEmpty()) {
            loadDirectory(_uiState.value.secondaryPane.currentPath.ifEmpty { rootPath }, PaneId.SECONDARY)
        }
    }

    fun setActivePane(paneId: PaneId) {
        _uiState.update { it.copy(activePaneId = paneId) }
    }

    fun copyFileToOtherPane(item: FileItem) {
        val targetPath = _uiState.value.targetPaneState.currentPath
        if (targetPath.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTransferring = true,
                    transferProgress = 0f,
                    transferMessage = "Copying ${item.name} to target pane..."
                )
            }

            val result = storageRepository.copyFile(
                sourcePathOrUri = item.path,
                targetParentPathOrUri = targetPath,
                resolution = FileConflictResolution.AUTO_RENAME
            ) { bytes, total ->
                val progress = if (total > 0) bytes.toFloat() / total.toFloat() else 0.5f
                _uiState.update { it.copy(transferProgress = progress) }
            }

            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            operationSuccessMessage = "Copied ${result.data.name} to other pane"
                        )
                    }
                    // Refresh target pane
                    loadDirectory(targetPath, getOtherPaneId(_uiState.value.activePaneId))
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            operationErrorMessage = result.throwable.localizedMessage ?: "Copy failed"
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun moveFileToOtherPane(item: FileItem) {
        val targetPath = _uiState.value.targetPaneState.currentPath
        val activePath = _uiState.value.activePaneState.currentPath
        if (targetPath.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTransferring = true,
                    transferProgress = 0f,
                    transferMessage = "Moving ${item.name} to target pane..."
                )
            }

            val result = storageRepository.moveFile(
                sourcePathOrUri = item.path,
                targetParentPathOrUri = targetPath,
                resolution = FileConflictResolution.AUTO_RENAME
            )

            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            operationSuccessMessage = "Moved ${result.data.name} to other pane"
                        )
                    }
                    // Refresh both panes
                    loadDirectory(activePath, _uiState.value.activePaneId)
                    loadDirectory(targetPath, getOtherPaneId(_uiState.value.activePaneId))
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isTransferring = false,
                            operationErrorMessage = result.throwable.localizedMessage ?: "Move failed"
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    private fun getOtherPaneId(paneId: PaneId): PaneId =
        if (paneId == PaneId.PRIMARY) PaneId.SECONDARY else PaneId.PRIMARY

    // ==========================================
    // DIRECTORY BROWSING & NAVIGATION
    // ==========================================

    fun loadDirectory(path: String, paneId: PaneId = _uiState.value.activePaneId) {
        viewModelScope.launch {
            updatePaneState(paneId) { it.copy(isLoading = true, errorMessage = null) }

            val sort = getPaneSortOption(paneId)
            val result = storageRepository.listFiles(path, sort)

            when (result) {
                is ResultWrapper.Success -> {
                    val breadcrumbs = buildBreadcrumbs(path)
                    updatePaneState(paneId) {
                        it.copy(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            files = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }

                    // Update active tab title and path if updating primary active pane
                    if (paneId == PaneId.PRIMARY) {
                        updateActiveTabInfo(path, getDirectoryDisplayName(path))
                    }
                }
                is ResultWrapper.Error -> {
                    updatePaneState(paneId) {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.localizedMessage ?: "Failed to read directory"
                        )
                    }
                }
                is ResultWrapper.Loading -> {
                    updatePaneState(paneId) { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun navigateInto(item: FileItem, paneId: PaneId = _uiState.value.activePaneId) {
        if (item.isDirectory) {
            if (_uiState.value.isSearching) {
                clearSearch()
            }
            loadDirectory(item.path, paneId)
        } else if (item.fileType == FileType.ARCHIVE) {
            viewArchive(item)
        }
    }

    fun navigateUp(paneId: PaneId = _uiState.value.activePaneId): Boolean {
        if (_uiState.value.isSearching) {
            clearSearch()
            return true
        }
        val current = getPaneCurrentPath(paneId)
        val parentFile = File(current).parentFile
        return if (parentFile != null && parentFile.exists() && parentFile.canRead()) {
            loadDirectory(parentFile.absolutePath, paneId)
            true
        } else {
            false
        }
    }

    fun navigateToBreadcrumb(targetPath: String, paneId: PaneId = _uiState.value.activePaneId) {
        if (_uiState.value.isSearching) {
            clearSearch()
        }
        if (targetPath != getPaneCurrentPath(paneId)) {
            loadDirectory(targetPath, paneId)
        }
    }

    fun setSortOption(option: FileSortOption, paneId: PaneId = _uiState.value.activePaneId) {
        updatePaneState(paneId) { it.copy(sortOption = option) }
        loadDirectory(getPaneCurrentPath(paneId), paneId)
        if (paneId == PaneId.PRIMARY) {
            updateActiveTabPreferences(sortOption = option)
        }
    }

    fun toggleViewMode(paneId: PaneId = _uiState.value.activePaneId) {
        updatePaneState(paneId) { it.copy(isGridView = !it.isGridView) }
        if (paneId == PaneId.PRIMARY) {
            updateActiveTabPreferences(isGridView = _uiState.value.primaryPane.isGridView)
        }
    }

    // ==========================================
    // ADVANCED SEARCH & HISTORY
    // ==========================================

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(query = query)) }
        performSearch()
    }

    fun setFileTypeFilter(type: FileType?) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(fileType = type)) }
        performSearch()
    }

    fun setSizeFilter(range: SizeFilterRange) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(sizeRange = range)) }
        performSearch()
    }

    fun setDateFilter(range: DateFilterRange) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(dateRange = range)) }
        performSearch()
    }

    fun setSearchTarget(target: SearchTarget) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(searchTarget = target)) }
        performSearch()
    }

    fun setSearchScope(scopePath: String?) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(scopePath = scopePath)) }
        performSearch()
    }

    fun setSearchSortOption(sort: FileSortOption) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(sortOption = sort)) }
        performSearch()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchCriteria = it.searchCriteria.copy(
                    fileType = null,
                    sizeRange = SizeFilterRange.ALL,
                    dateRange = DateFilterRange.ALL,
                    searchTarget = SearchTarget.BOTH,
                    scopePath = null,
                    extension = null
                )
            )
        }
        performSearch()
    }

    fun toggleFilterSheet(show: Boolean) {
        _uiState.update { it.copy(showFilterSheet = show) }
    }

    fun performSearch() {
        searchJob?.cancel()
        val criteria = _uiState.value.searchCriteria
        val isQueryEmpty = criteria.query.isBlank()
        val hasActiveFilters = criteria.isFilteringActive

        if (isQueryEmpty && !hasActiveFilters) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }

        // Record query in history if not empty
        if (!isQueryEmpty) {
            viewModelScope.launch {
                searchHistoryRepository.addSearchQuery(criteria.query)
            }
        }

        searchJob = viewModelScope.launch {
            fileIndexingRepository.searchFilesAdvanced(criteria).collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchCriteria = SearchFilterCriteria(),
                isSearching = false,
                searchResults = emptyList(),
                showFilterSheet = false
            )
        }
    }

    fun rerunRecentSearch(query: String) {
        _uiState.update { it.copy(searchCriteria = it.searchCriteria.copy(query = query)) }
        performSearch()
    }

    fun removeSearchFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    fun navigateToParentFolder(item: FileItem) {
        clearSearch()
        val parentPath = item.parentPath
        if (parentPath.isNotBlank()) {
            loadDirectory(parentPath, _uiState.value.activePaneId)
        }
    }

    fun openSearchResultInNewTab(item: FileItem) {
        val targetPath = if (item.isDirectory) item.path else item.parentPath
        if (targetPath.isNotBlank()) {
            clearSearch()
            openNewTab(targetPath, item.name)
        }
    }

    // ==========================================
    // CRUD OPERATIONS
    // ==========================================

    fun createFolder(name: String, paneId: PaneId = _uiState.value.activePaneId) {
        viewModelScope.launch {
            val current = getPaneCurrentPath(paneId)
            val result = storageRepository.createDirectory(current, name.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showCreateFolderDialog = false, operationSuccessMessage = "Folder created: ${result.data.name}") }
                    loadDirectory(current, paneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(operationErrorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun createFile(name: String, paneId: PaneId = _uiState.value.activePaneId) {
        viewModelScope.launch {
            val current = getPaneCurrentPath(paneId)
            val result = storageRepository.createFile(current, name.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showCreateFileDialog = false, operationSuccessMessage = "File created: ${result.data.name}") }
                    loadDirectory(current, paneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(operationErrorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun renameFile(item: FileItem, newName: String, paneId: PaneId = _uiState.value.activePaneId) {
        viewModelScope.launch {
            val result = storageRepository.renameFile(item.path, newName.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(itemToRename = null, operationSuccessMessage = "Renamed to: ${result.data.name}") }
                    loadDirectory(getPaneCurrentPath(paneId), paneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(operationErrorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun deleteFile(item: FileItem, paneId: PaneId = _uiState.value.activePaneId) {
        viewModelScope.launch {
            val result = storageRepository.deleteFile(item.path)
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(itemToDelete = null, operationSuccessMessage = "Deleted: ${item.name}") }
                    loadDirectory(getPaneCurrentPath(paneId), paneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(operationErrorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    // Indexing triggers
    fun triggerManualIndexing(forceRescan: Boolean = false) {
        viewModelScope.launch {
            fileIndexingRepository.runIndexing(forceRescan)
        }
    }

    fun cancelIndexing() {
        fileIndexingRepository.cancelIndexing()
    }

    // Dialog state helpers
    fun openCreateFolderDialog() = _uiState.update { it.copy(showCreateFolderDialog = true) }
    fun closeCreateFolderDialog() = _uiState.update { it.copy(showCreateFolderDialog = false) }

    fun openCreateFileDialog() = _uiState.update { it.copy(showCreateFileDialog = true) }
    fun closeCreateFileDialog() = _uiState.update { it.copy(showCreateFileDialog = false) }

    fun openRenameDialog(item: FileItem) = _uiState.update { it.copy(itemToRename = item) }
    fun closeRenameDialog() = _uiState.update { it.copy(itemToRename = null) }

    fun openDeleteDialog(item: FileItem) = _uiState.update { it.copy(itemToDelete = item) }
    fun closeDeleteDialog() = _uiState.update { it.copy(itemToDelete = null) }

    fun selectItemForDetails(item: FileItem?, paneId: PaneId = _uiState.value.activePaneId) {
        updatePaneState(paneId) { it.copy(selectedItemForDetails = item) }
    }

    fun clearOperationMessage() = _uiState.update {
        it.copy(operationSuccessMessage = null, operationErrorMessage = null)
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private fun updatePaneState(paneId: PaneId, transform: (PaneState) -> PaneState) {
        _uiState.update {
            if (paneId == PaneId.PRIMARY) {
                it.copy(primaryPane = transform(it.primaryPane))
            } else {
                it.copy(secondaryPane = transform(it.secondaryPane))
            }
        }
    }

    private fun getPaneCurrentPath(paneId: PaneId): String =
        if (paneId == PaneId.PRIMARY) _uiState.value.primaryPane.currentPath else _uiState.value.secondaryPane.currentPath

    private fun getPaneSortOption(paneId: PaneId): FileSortOption =
        if (paneId == PaneId.PRIMARY) _uiState.value.primaryPane.sortOption else _uiState.value.secondaryPane.sortOption

    private fun updateActiveTabInfo(path: String, title: String) {
        val state = _uiState.value
        val activeTab = state.activeTab ?: return
        if (activeTab.path == path && state.activeTabIndex in state.tabs.indices) {
            val updated = state.tabs.toMutableList()
            updated[state.activeTabIndex] = activeTab.copy(path = path, title = title)
            _uiState.update { it.copy(tabs = updated) }
            persistTabs()
        }
    }

    private fun updateActiveTabPreferences(isGridView: Boolean? = null, sortOption: FileSortOption? = null) {
        val state = _uiState.value
        if (state.tabs.isNotEmpty() && state.activeTabIndex in state.tabs.indices) {
            val currentTab = state.tabs[state.activeTabIndex]
            val updated = state.tabs.toMutableList()
            updated[state.activeTabIndex] = currentTab.copy(
                isGridView = isGridView ?: currentTab.isGridView,
                sortOption = sortOption ?: currentTab.sortOption
            )
            _uiState.update { it.copy(tabs = updated) }
            persistTabs()
        }
    }

    private fun getDirectoryDisplayName(path: String): String {
        val file = File(path)
        return when {
            path == rootPath || path == "/" -> "Storage"
            file.name.isNotEmpty() -> file.name
            else -> "Storage"
        }
    }

    private fun buildBreadcrumbs(path: String): List<Pair<String, String>> {
        val crumbs = mutableListOf<Pair<String, String>>()
        var current = File(path)
        while (current.parentFile != null && current.absolutePath != "/") {
            crumbs.add(0, (if (current.name.isEmpty()) "Root" else current.name) to current.absolutePath)
            current = current.parentFile ?: break
        }
        crumbs.add(0, "Storage" to rootPath)
        return crumbs
    }

    // Archive Management Operations
    fun showCreateArchiveDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateArchiveDialog = show) }
    }

    fun showExtractArchiveDialog(file: FileItem?) {
        _uiState.update { it.copy(archiveFileToExtract = file) }
    }

    fun viewArchive(file: FileItem?) {
        _uiState.update { it.copy(archiveFileToView = file) }
    }

    fun createArchive(
        name: String,
        format: com.xfiles.pro.domain.model.ArchiveFormat,
        level: com.xfiles.pro.domain.model.CompressionLevel,
        password: String?
    ) {
        val repo = archiveRepository ?: return
        val currentPane = _uiState.value.activePaneState
        val selectedFiles: List<File> = currentPane.selectedItemForAction?.let { listOf(File(it.path)) }
            ?: listOf(File(currentPane.currentPath))
        val targetFile = File(currentPane.currentPath, "$name.${format.extension}")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showCreateArchiveDialog = false,
                    isArchiveOperationInProgress = true,
                    archiveProgress = com.xfiles.pro.domain.model.ArchiveProgress(currentItem = targetFile.name, percentage = 0f)
                )
            }

            val result = repo.createArchive(
                sourceFiles = selectedFiles,
                destinationArchiveFile = targetFile,
                format = format,
                compressionLevel = level,
                password = password,
                onProgress = { progress ->
                    _uiState.update { it.copy(archiveProgress = progress) }
                }
            )

            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isArchiveOperationInProgress = false,
                            archiveProgress = null,
                            operationSuccessMessage = "Archive created: ${targetFile.name}"
                        )
                    }
                    loadDirectory(_uiState.value.activePaneState.currentPath, _uiState.value.activePaneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isArchiveOperationInProgress = false,
                            archiveProgress = null,
                            operationErrorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun extractArchive(
        destinationDir: File,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        selectedOnly: Boolean = false,
        password: String? = null
    ) {
        val repo = archiveRepository ?: return
        val archiveItem = _uiState.value.archiveFileToExtract ?: return
        val archiveFile = File(archiveItem.path)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    archiveFileToExtract = null,
                    isArchiveOperationInProgress = true,
                    archiveProgress = com.xfiles.pro.domain.model.ArchiveProgress(currentItem = archiveFile.name, percentage = 0f)
                )
            }

            val result = repo.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                password = password,
                conflictResolution = conflictResolution,
                onProgress = { progress ->
                    _uiState.update { it.copy(archiveProgress = progress) }
                }
            )

            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isArchiveOperationInProgress = false,
                            archiveProgress = null,
                            operationSuccessMessage = "Extracted ${result.data} files successfully"
                        )
                    }
                    loadDirectory(_uiState.value.activePaneState.currentPath, _uiState.value.activePaneId)
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isArchiveOperationInProgress = false,
                            archiveProgress = null,
                            operationErrorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun cancelArchiveOperation() {
        _uiState.update {
            it.copy(
                isArchiveOperationInProgress = false,
                archiveProgress = null,
                operationErrorMessage = "Archive operation cancelled"
            )
        }
    }

    companion object {
        fun provideFactory(
            storageRepository: StorageRepository,
            fileIndexingRepository: FileIndexingRepository,
            tabRepository: TabRepository,
            searchHistoryRepository: SearchHistoryRepository,
            archiveRepository: com.xfiles.pro.domain.repository.ArchiveRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FilesViewModel(
                        storageRepository,
                        fileIndexingRepository,
                        tabRepository,
                        searchHistoryRepository,
                        archiveRepository
                    ) as T
                }
            }
    }
}
