package com.xfiles.pro.ui.theme

import com.xfiles.pro.domain.model.ThemeMode

/**
 * UI State for Theme management.
 */
data class ThemeUiState(
    val currentTheme: ThemeMode = ThemeMode.CLASSIC,
    val pendingSelectedTheme: ThemeMode = ThemeMode.CLASSIC,
    val isThemeDialogVisible: Boolean = false,
    val isLoading: Boolean = false
)
