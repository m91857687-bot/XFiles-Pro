package com.xfiles.pro.ui.files

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.repository.FileIndexingRepository
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel managing file browsing, navigation stack, CRUD operations,
 * instant FTS search, and persistent indexing status.
 */
class FilesViewModel(
    private val storageRepository: StorageRepository,
    private val fileIndexingRepository: FileIndexingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private val rootPath: String by lazy {
        Environment.getExternalStorageDirectory().absolutePath
    }

    init {
        // Initialize with default storage root
        loadDirectory(rootPath)

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

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = storageRepository.listFiles(path, _uiState.value.sortOption)
            when (result) {
                is ResultWrapper.Success -> {
                    val breadcrumbs = buildBreadcrumbs(path)
                    _uiState.update {
                        it.copy(
                            currentPath = path,
                            breadcrumbs = breadcrumbs,
                            files = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.localizedMessage ?: "Failed to read directory"
                        )
                    }
                }
                is ResultWrapper.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun navigateInto(item: FileItem) {
        if (item.isDirectory) {
            if (_uiState.value.isSearching) {
                clearSearch()
            }
            loadDirectory(item.path)
        }
    }

    fun navigateUp(): Boolean {
        if (_uiState.value.isSearching) {
            clearSearch()
            return true
        }
        val current = _uiState.value.currentPath
        val parentFile = File(current).parentFile
        return if (parentFile != null && parentFile.exists() && parentFile.canRead()) {
            loadDirectory(parentFile.absolutePath)
            true
        } else {
            false
        }
    }

    fun navigateToBreadcrumb(targetPath: String) {
        if (_uiState.value.isSearching) {
            clearSearch()
        }
        if (targetPath != _uiState.value.currentPath) {
            loadDirectory(targetPath)
        }
    }

    fun setSortOption(option: FileSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        loadDirectory(_uiState.value.currentPath)
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    // Phase 5B: Full Text Search (FTS) operations
    fun onSearchQueryChange(query: String, filterType: FileType? = null) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            fileIndexingRepository.searchFiles(query, filterType).collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = emptyList()
            )
        }
    }

    // Phase 5B: Indexing manual trigger and cancellation
    fun triggerManualIndexing(forceRescan: Boolean = false) {
        viewModelScope.launch {
            fileIndexingRepository.runIndexing(forceRescan)
        }
    }

    fun cancelIndexing() {
        fileIndexingRepository.cancelIndexing()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val current = _uiState.value.currentPath
            val result = storageRepository.createDirectory(current, name.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showCreateFolderDialog = false, operationSuccessMessage = "Folder created: ${result.data.name}") }
                    loadDirectory(current)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(errorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val current = _uiState.value.currentPath
            val result = storageRepository.createFile(current, name.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showCreateFileDialog = false, operationSuccessMessage = "File created: ${result.data.name}") }
                    loadDirectory(current)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(errorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val result = storageRepository.renameFile(item.path, newName.trim())
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(itemToRename = null, operationSuccessMessage = "Renamed to: ${result.data.name}") }
                    loadDirectory(_uiState.value.currentPath)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(errorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    fun deleteFile(item: FileItem) {
        viewModelScope.launch {
            val result = storageRepository.deleteFile(item.path)
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(itemToDelete = null, operationSuccessMessage = "Deleted: ${item.name}") }
                    loadDirectory(_uiState.value.currentPath)
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(errorMessage = result.throwable.localizedMessage) }
                }
                else -> Unit
            }
        }
    }

    // Dialog state management
    fun openCreateFolderDialog() = _uiState.update { it.copy(showCreateFolderDialog = true) }
    fun closeCreateFolderDialog() = _uiState.update { it.copy(showCreateFolderDialog = false) }

    fun openCreateFileDialog() = _uiState.update { it.copy(showCreateFileDialog = true) }
    fun closeCreateFileDialog() = _uiState.update { it.copy(showCreateFileDialog = false) }

    fun openRenameDialog(item: FileItem) = _uiState.update { it.copy(itemToRename = item) }
    fun closeRenameDialog() = _uiState.update { it.copy(itemToRename = null) }

    fun openDeleteDialog(item: FileItem) = _uiState.update { it.copy(itemToDelete = item) }
    fun closeDeleteDialog() = _uiState.update { it.copy(itemToDelete = null) }

    fun selectItemForDetails(item: FileItem?) = _uiState.update { it.copy(selectedItemForDetails = item) }
    fun clearOperationMessage() = _uiState.update { it.copy(operationSuccessMessage = null, errorMessage = null) }

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

    companion object {
        fun provideFactory(
            storageRepository: StorageRepository,
            fileIndexingRepository: FileIndexingRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FilesViewModel(storageRepository, fileIndexingRepository) as T
                }
            }
    }
}
