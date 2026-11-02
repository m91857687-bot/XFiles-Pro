package com.xfiles.pro.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore persistence layer for recent search queries.
 * Reuses the existing Context.dataStore instance.
 */
class SearchPreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_search_queries")
        private const val MAX_RECENT_SEARCHES = 15
    }

    val recentSearches: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[KEY_RECENT_SEARCHES]
            if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                raw.lines().filter { it.isNotBlank() }
            }
        }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        try {
            dataStore.edit { preferences ->
                val current = preferences[KEY_RECENT_SEARCHES]?.lines()?.filter { it.isNotBlank() } ?: emptyList()
                val updated = (listOf(trimmed) + current.filter { !it.equals(trimmed, ignoreCase = true) })
                    .take(MAX_RECENT_SEARCHES)
                preferences[KEY_RECENT_SEARCHES] = updated.joinToString("\n")
            }
        } catch (_: IOException) {
            // Ignore I/O failure
        }
    }

    suspend fun removeSearchQuery(query: String) {
        val trimmed = query.trim()
        try {
            dataStore.edit { preferences ->
                val current = preferences[KEY_RECENT_SEARCHES]?.lines()?.filter { it.isNotBlank() } ?: emptyList()
                val updated = current.filter { !it.equals(trimmed, ignoreCase = true) }
                preferences[KEY_RECENT_SEARCHES] = updated.joinToString("\n")
            }
        } catch (_: IOException) {
            // Ignore I/O failure
        }
    }

    suspend fun clearHistory() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(KEY_RECENT_SEARCHES)
            }
        } catch (_: IOException) {
            // Ignore I/O failure
        }
    }
}
