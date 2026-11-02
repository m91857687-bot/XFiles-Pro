package com.xfiles.pro.domain.repository

import com.xfiles.pro.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing and observing application visual theme preferences.
 */
interface ThemeRepository {
    /**
     * Flow emitting the currently selected ThemeMode.
     */
    val selectedTheme: Flow<ThemeMode>

    /**
     * Persists the user's chosen ThemeMode.
     */
    suspend fun setTheme(theme: ThemeMode)
}
