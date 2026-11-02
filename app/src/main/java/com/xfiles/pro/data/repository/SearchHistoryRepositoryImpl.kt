package com.xfiles.pro.data.repository

import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.datastore.SearchPreferences
import com.xfiles.pro.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of SearchHistoryRepository backed by DataStore on Dispatchers.IO.
 */
class SearchHistoryRepositoryImpl(
    private val searchPreferences: SearchPreferences,
    private val dispatchers: DispatchersProvider
) : SearchHistoryRepository {

    override val recentSearches: Flow<List<String>> =
        searchPreferences.recentSearches.flowOn(dispatchers.io)

    override suspend fun addSearchQuery(query: String) {
        withContext(dispatchers.io) {
            searchPreferences.addSearchQuery(query)
        }
    }

    override suspend fun removeSearchQuery(query: String) {
        withContext(dispatchers.io) {
            searchPreferences.removeSearchQuery(query)
        }
    }

    override suspend fun clearHistory() {
        withContext(dispatchers.io) {
            searchPreferences.clearHistory()
        }
    }
}
