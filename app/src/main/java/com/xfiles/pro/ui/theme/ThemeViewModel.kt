package com.xfiles.pro.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xfiles.pro.domain.model.ThemeMode
import com.xfiles.pro.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing theme preferences, runtime switching, and dialog state.
 */
class ThemeViewModel(
    private val repository: ThemeRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(
        DialogState(isVisible = false, pendingTheme = ThemeMode.CLASSIC)
    )

    private data class DialogState(
        val isVisible: Boolean,
        val pendingTheme: ThemeMode
    )

    val uiState: StateFlow<ThemeUiState> = combine(
        repository.selectedTheme,
        _dialogState
    ) { theme, dialogState ->
        ThemeUiState(
            currentTheme = theme,
            pendingSelectedTheme = dialogState.pendingTheme,
            isThemeDialogVisible = dialogState.isVisible,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeUiState(isLoading = true)
    )

    fun onOpenThemeDialog() {
        val current = uiState.value.currentTheme
        _dialogState.value = DialogState(isVisible = true, pendingTheme = current)
    }

    fun onSelectPendingTheme(theme: ThemeMode) {
        _dialogState.update { it.copy(pendingTheme = theme) }
    }

    fun onCloseThemeDialog() {
        _dialogState.update { it.copy(isVisible = false) }
    }

    fun onApplyTheme() {
        val target = _dialogState.value.pendingTheme
        viewModelScope.launch {
            repository.setTheme(target)
            _dialogState.update { it.copy(isVisible = false) }
        }
    }

    fun onSetThemeDirectly(theme: ThemeMode) {
        viewModelScope.launch {
            repository.setTheme(theme)
            _dialogState.update { it.copy(pendingTheme = theme, isVisible = false) }
        }
    }

    companion object {
        fun provideFactory(repository: ThemeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ThemeViewModel(repository) as T
                }
            }
    }
}
