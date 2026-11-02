package com.xfiles.pro.ui.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.domain.model.AppLanguage
import com.xfiles.pro.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing language state, first-run tracking, and dialog visibility.
 */
class LanguageViewModel(
    private val repository: LanguageRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(
        DialogState(isVisible = false, pendingLanguage = AppLanguage.ARABIC)
    )

    private data class DialogState(
        val isVisible: Boolean,
        val pendingLanguage: AppLanguage
    )

    val uiState: StateFlow<LanguageUiState> = combine(
        repository.selectedLanguage,
        repository.isFirstRunCompleted,
        _dialogState
    ) { language, isFirstRunDone, dialogState ->
        LanguageUiState(
            currentLanguage = language,
            selectedPendingLanguage = dialogState.pendingLanguage,
            isFirstRunCompleted = isFirstRunDone,
            isLoading = false,
            isLanguageDialogVisible = dialogState.isVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LanguageUiState(isLoading = true)
    )

    fun onSelectLanguage(language: AppLanguage) {
        _dialogState.update { it.copy(pendingLanguage = language) }
    }

    fun onConfirmFirstRun(language: AppLanguage) {
        viewModelScope.launch {
            repository.completeFirstRun(language)
        }
    }

    fun onOpenLanguageDialog() {
        val currentLang = uiState.value.currentLanguage
        _dialogState.value = DialogState(isVisible = true, pendingLanguage = currentLang)
    }

    fun onCloseLanguageDialog() {
        _dialogState.update { it.copy(isVisible = false) }
    }

    fun onApplyDialogLanguage() {
        val targetLang = _dialogState.value.pendingLanguage
        viewModelScope.launch {
            repository.setLanguage(targetLang)
            _dialogState.update { it.copy(isVisible = false) }
        }
    }

    companion object {
        fun provideFactory(repository: LanguageRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LanguageViewModel(repository) as T
                }
            }
    }
}
