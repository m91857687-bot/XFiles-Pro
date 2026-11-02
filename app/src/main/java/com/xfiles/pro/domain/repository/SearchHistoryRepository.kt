package com.xfiles.pro.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for managing recent search history queries.
 */
interface SearchHistoryRepository {
    /**
     * Flow of recent search queries ordered from newest to oldest.
     */
    val recentSearches: Flow<List<String>>

    /**
     * Add a search term to history, moving it to top if already present.
     */
    suspend fun addSearchQuery(query: String)

    /**
     * Remove a specific search term from history.
     */
    suspend fun removeSearchQuery(query: String)

    /**
     * Clear all search history.
     */
    suspend fun clearHistory()
}
