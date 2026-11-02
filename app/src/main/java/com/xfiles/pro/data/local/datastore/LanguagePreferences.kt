package com.xfiles.pro.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xfiles.pro.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xfiles_preferences")

/**
 * DataStore persistence layer for application language and first-run status.
 */
class LanguagePreferences(private val context: Context) {

    companion object {
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language_code")
        val KEY_FIRST_RUN_COMPLETED = booleanPreferencesKey("is_first_run_completed")
    }

    val selectedLanguage: Flow<AppLanguage> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val code = preferences[KEY_SELECTED_LANGUAGE]
            if (code != null) {
                AppLanguage.fromCode(code)
            } else {
                AppLanguage.ARABIC // Default locale before explicit choice
            }
        }

    val isFirstRunCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FIRST_RUN_COMPLETED] ?: false
        }

    suspend fun saveLanguage(language: AppLanguage) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_SELECTED_LANGUAGE] = language.code
            }
        } catch (_: IOException) {
            // Gracefully ignore write failures in corrupted storage situations
        }
    }

    suspend fun completeFirstRun(language: AppLanguage) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_SELECTED_LANGUAGE] = language.code
                preferences[KEY_FIRST_RUN_COMPLETED] = true
            }
        } catch (_: IOException) {
            // Gracefully ignore write failures in corrupted storage situations
        }
    }
}
