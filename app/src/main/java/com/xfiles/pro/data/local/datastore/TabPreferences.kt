package com.xfiles.pro.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileTab
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore persistence layer for multi-tab browsing sessions.
 * Reuses the existing Context.dataStore instance.
 */
class TabPreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val KEY_TABS_DATA = stringPreferencesKey("saved_tabs_data")
        val KEY_ACTIVE_TAB_INDEX = intPreferencesKey("active_tab_index")
    }

    val savedTabs: Flow<List<FileTab>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val serialized = preferences[KEY_TABS_DATA]
            parseTabs(serialized)
        }

    val activeTabIndex: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_ACTIVE_TAB_INDEX] ?: 0
        }

    suspend fun saveTabs(tabs: List<FileTab>, activeIndex: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_TABS_DATA] = serializeTabs(tabs)
                preferences[KEY_ACTIVE_TAB_INDEX] = activeIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
            }
        } catch (_: IOException) {
            // Gracefully ignore I/O errors during preferences write
        }
    }

    private fun serializeTabs(tabs: List<FileTab>): String {
        return tabs.joinToString("\n") { tab ->
            "${tab.id}\t${tab.title}\t${tab.path}\t${tab.isGridView}\t${tab.sortOption.name}\t${tab.createdAt}"
        }
    }

    private fun parseTabs(serialized: String?): List<FileTab> {
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size >= 3) {
                val id = parts[0]
                val title = parts[1]
                val path = parts[2]
                val isGridView = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false
                val sortOption = parts.getOrNull(4)?.let {
                    try { FileSortOption.valueOf(it) } catch (_: Exception) { FileSortOption.NAME_ASC }
                } ?: FileSortOption.NAME_ASC
                val createdAt = parts.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()

                FileTab(
                    id = id,
                    title = title,
                    path = path,
                    isGridView = isGridView,
                    sortOption = sortOption,
                    createdAt = createdAt
                )
            } else {
                null
            }
        }
    }
}
