package com.xfiles.pro.ui.download

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.di.AppContainer
import com.xfiles.pro.domain.model.DownloadItem
import com.xfiles.pro.domain.model.DownloadRequest
import com.xfiles.pro.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for managing download tasks, reactive state updates, and user interactions.
 */
class DownloadViewModel(
    private val downloadRepository: DownloadRepository,
    defaultDownloadDir: String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        ?: "/storage/emulated/0/Download"
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DownloadUiState(defaultDestinationDirectory = defaultDownloadDir)
    )
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.getAllDownloads().collect { list ->
                _uiState.update { it.copy(items = list) }
            }
        }
    }

    fun setFilter(filter: DownloadFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openNewDownloadDialog() {
        _uiState.update { it.copy(showNewDownloadDialog = true) }
    }

    fun closeNewDownloadDialog() {
        _uiState.update { it.copy(showNewDownloadDialog = false) }
    }

    fun startDownload(url: String, fileName: String?, destinationDir: String) {
        if (url.isBlank()) {
            _uiState.update { it.copy(userMessage = "URL cannot be empty") }
            return
        }

        val targetDir = destinationDir.ifBlank { _uiState.value.defaultDestinationDirectory }

        viewModelScope.launch {
            val result = downloadRepository.enqueueDownload(
                DownloadRequest(
                    url = url.trim(),
                    fileName = fileName?.trim()?.ifBlank { null },
                    destinationDirectory = targetDir
                )
            )
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showNewDownloadDialog = false, userMessage = null) }
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(userMessage = result.message) }
                }
                is ResultWrapper.Loading -> {}
            }
        }
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(id)
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(id)
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(id)
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            downloadRepository.retryDownload(id)
        }
    }

    fun openDetails(item: DownloadItem) {
        _uiState.update { it.copy(selectedItem = item, showDetailsDialog = true) }
    }

    fun closeDetails() {
        _uiState.update { it.copy(showDetailsDialog = false, selectedItem = null) }
    }

    fun openRename(item: DownloadItem) {
        _uiState.update { it.copy(selectedItem = item, showRenameDialog = true) }
    }

    fun closeRename() {
        _uiState.update { it.copy(showRenameDialog = false, selectedItem = null) }
    }

    fun renameItem(id: String, newName: String) {
        viewModelScope.launch {
            val result = downloadRepository.renameCompletedDownload(id, newName)
            when (result) {
                is ResultWrapper.Success -> {
                    _uiState.update { it.copy(showRenameDialog = false, selectedItem = null) }
                }
                is ResultWrapper.Error -> {
                    _uiState.update { it.copy(userMessage = result.message) }
                }
                is ResultWrapper.Loading -> {}
            }
        }
    }

    fun openDeleteConfirm(item: DownloadItem) {
        _uiState.update { it.copy(selectedItem = item, showDeleteConfirmDialog = true, deleteWithFile = false) }
    }

    fun closeDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, selectedItem = null) }
    }

    fun setDeleteWithFile(deleteFile: Boolean) {
        _uiState.update { it.copy(deleteWithFile = deleteFile) }
    }

    fun confirmDelete(id: String, deleteFile: Boolean) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(id, deleteFile)
            _uiState.update { it.copy(showDeleteConfirmDialog = false, selectedItem = null) }
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompletedDownloads()
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val defaultDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )?.absolutePath ?: "/storage/emulated/0/Download"

                    return DownloadViewModel(
                        downloadRepository = container.downloadRepository,
                        defaultDownloadDir = defaultDir
                    ) as T
                }
            }
    }
}
