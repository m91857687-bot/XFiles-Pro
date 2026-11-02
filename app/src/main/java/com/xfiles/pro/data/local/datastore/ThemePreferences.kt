package com.xfiles.pro.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xfiles.pro.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore persistence layer for application visual theme preferences.
 * Reuses the existing Context.dataStore instance from LanguagePreferences to avoid duplicate delegates.
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val KEY_SELECTED_THEME = stringPreferencesKey("selected_theme_mode")
    }

    val selectedTheme: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeId = preferences[KEY_SELECTED_THEME]
            ThemeMode.fromId(themeId)
        }

    suspend fun saveTheme(theme: ThemeMode) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_SELECTED_THEME] = theme.id
            }
        } catch (_: IOException) {
            // Gracefully ignore I/O errors during preferences commit
        }
    }
}
