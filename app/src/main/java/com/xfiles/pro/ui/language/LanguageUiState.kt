package com.xfiles.pro.ui.language

import com.xfiles.pro.domain.model.AppLanguage

/**
 * UI State for language selection and runtime switching.
 */
data class LanguageUiState(
    val currentLanguage: AppLanguage = AppLanguage.ARABIC,
    val selectedPendingLanguage: AppLanguage = AppLanguage.ARABIC,
    val isFirstRunCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val isLanguageDialogVisible: Boolean = false
)
