package com.xfiles.pro.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.repository.ArchiveRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for browsing and operating on archive files.
 */
class ArchiveViewModel(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun loadArchive(file: File, password: String? = null) {
        val format = ArchiveFormat.detectFormat(file.name)
        _uiState.update {
            it.copy(
                archiveFile = file,
                format = format,
                currentVirtualPath = "",
                isLoading = true,
                errorMessage = null,
                password = password,
                showPasswordPrompt = false
            )
        }

        viewModelScope.launch {
            val isEncrypted = archiveRepository.isPasswordProtected(file)
            _uiState.update { it.copy(isEncrypted = isEncrypted) }

            if (isEncrypted && password.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showPasswordPrompt = true
                    )
                }
                return@launch
            }

            when (val result = archiveRepository.listEntries(file, password)) {
                is ResultWrapper.Success -> {
                    val entries = result.data
                    _uiState.update { current ->
                        current.copy(
                            allEntries = entries,
                            isLoading = false,
                            visibleEntries = computeVisibleEntries(entries, "", current.searchQuery)
                        )
                    }
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            showPasswordPrompt = isEncrypted
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun enterVirtualFolder(folderPath: String) {
        val cleanPath = folderPath.trimEnd('/')
        _uiState.update { current ->
            current.copy(
                currentVirtualPath = cleanPath,
                selectedEntryPaths = emptySet(),
                visibleEntries = computeVisibleEntries(current.allEntries, cleanPath, current.searchQuery)
            )
        }
    }

    fun navigateUp(): Boolean {
        val current = _uiState.value.currentVirtualPath
        if (current.isEmpty()) return false

        val parent = if (current.contains('/')) current.substringBeforeLast('/') else ""
        _uiState.update { state ->
            state.copy(
                currentVirtualPath = parent,
                selectedEntryPaths = emptySet(),
                visibleEntries = computeVisibleEntries(state.allEntries, parent, state.searchQuery)
            )
        }
        return true
    }

    fun navigateToBreadcrumb(path: String) {
        _uiState.update { state ->
            state.copy(
                currentVirtualPath = path,
                selectedEntryPaths = emptySet(),
                visibleEntries = computeVisibleEntries(state.allEntries, path, state.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                visibleEntries = computeVisibleEntries(current.allEntries, current.currentVirtualPath, query)
            )
        }
    }

    fun toggleSelection(entryPath: String) {
        _uiState.update { current ->
            val set = current.selectedEntryPaths.toMutableSet()
            if (set.contains(entryPath)) {
                set.remove(entryPath)
            } else {
                set.add(entryPath)
            }
            current.copy(selectedEntryPaths = set)
        }
    }

    fun selectAll() {
        _uiState.update { current ->
            val allPaths = current.visibleEntries.map { it.path }.toSet()
            current.copy(selectedEntryPaths = allPaths)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedEntryPaths = emptySet()) }
    }

    fun showExtractDialog(show: Boolean) {
        _uiState.update { it.copy(showExtractDialog = show) }
    }

    fun showPasswordPrompt(show: Boolean) {
        _uiState.update { it.copy(showPasswordPrompt = show) }
    }

    fun extract(
        destinationDir: File,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        selectedOnly: Boolean = false
    ) {
        val archive = _uiState.value.archiveFile ?: return
        val selected = if (selectedOnly) _uiState.value.selectedEntryPaths.toList() else null

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    showExtractDialog = false,
                    progress = ArchiveProgress(currentItem = archive.name, percentage = 0f)
                )
            }

            val result = archiveRepository.extractArchive(
                archiveFile = archive,
                destinationDir = destinationDir,
                password = _uiState.value.password,
                selectedEntries = selected,
                conflictResolution = conflictResolution,
                onProgress = { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            )

            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            progress = null,
                            operationSuccessMessage = "Successfully extracted ${result.data} files to ${destinationDir.name}"
                        )
                    }
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            progress = null,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun deleteSelectedEntries() {
        val archive = _uiState.value.archiveFile ?: return
        val selected = _uiState.value.selectedEntryPaths.toList()
        if (selected.isEmpty()) return

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOperationInProgress = true,
                    progress = ArchiveProgress(currentItem = "Deleting entries...", percentage = 0.5f)
                )
            }

            val result = archiveRepository.deleteEntriesFromZip(archive, selected)
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            progress = null,
                            selectedEntryPaths = emptySet(),
                            operationSuccessMessage = "Deleted ${selected.size} items"
                        )
                    }
                    loadArchive(archive, _uiState.value.password)
                }
                is ResultWrapper.Error -> {
                    _uiState.update {
                        it.copy(
                            isOperationInProgress = false,
                            progress = null,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun cancelOperation() {
        activeJob?.cancel()
        _uiState.update {
            it.copy(
                isOperationInProgress = false,
                progress = null,
                errorMessage = "Operation cancelled"
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(operationSuccessMessage = null, errorMessage = null) }
    }

    /**
     * Synthesizes hierarchical virtual directory contents from the flat archive entry list.
     */
    private fun computeVisibleEntries(
        entries: List<ArchiveEntryItem>,
        currentPath: String,
        query: String
    ): List<ArchiveEntryItem> {
        val cleanCurrent = currentPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }

        if (query.isNotBlank()) {
            val q = query.lowercase()
            return entries.filter { it.name.lowercase().contains(q) || it.path.lowercase().contains(q) }
                .sortedWith(compareByDescending<ArchiveEntryItem> { it.isDirectory }.thenBy { it.name.lowercase() })
        }

        val directDirs = mutableMapOf<String, ArchiveEntryItem>()
        val directFiles = mutableListOf<ArchiveEntryItem>()

        for (item in entries) {
            val path = item.path.trim('/')
            if (cleanCurrent.isNotEmpty() && !path.startsWith(cleanCurrent)) {
                continue
            }

            val relative = if (cleanCurrent.isEmpty()) path else path.removePrefix(cleanCurrent)
            val parts = relative.split('/')

            if (parts.size > 1) {
                // Sub-directory
                val dirName = parts[0]
                val dirVirtualPath = if (cleanCurrent.isEmpty()) dirName else "${cleanCurrent}$dirName"
                if (!directDirs.containsKey(dirName)) {
                    directDirs[dirName] = ArchiveEntryItem(
                        path = dirVirtualPath,
                        name = dirName,
                        isDirectory = true,
                        uncompressedSize = 0L,
                        compressedSize = 0L,
                        lastModified = item.lastModified
                    )
                }
            } else if (parts.size == 1 && parts[0].isNotEmpty()) {
                if (item.isDirectory) {
                    directDirs[parts[0]] = item
                } else {
                    directFiles.add(item)
                }
            }
        }

        val result = mutableListOf<ArchiveEntryItem>()
        result.addAll(directDirs.values.sortedBy { it.name.lowercase() })
        result.addAll(directFiles.sortedBy { it.name.lowercase() })
        return result
    }
}
