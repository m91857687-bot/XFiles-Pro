package com.xfiles.pro.data.repository

import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.data.local.datastore.TabPreferences
import com.xfiles.pro.domain.model.FileTab
import com.xfiles.pro.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of TabRepository backed by DataStore on Dispatchers.IO.
 */
class TabRepositoryImpl(
    private val tabPreferences: TabPreferences,
    private val dispatchers: DispatchersProvider
) : TabRepository {

    override val savedTabs: Flow<List<FileTab>> =
        tabPreferences.savedTabs.flowOn(dispatchers.io)

    override val activeTabIndex: Flow<Int> =
        tabPreferences.activeTabIndex.flowOn(dispatchers.io)

    override suspend fun saveTabs(tabs: List<FileTab>, activeIndex: Int) {
        withContext(dispatchers.io) {
            tabPreferences.saveTabs(tabs, activeIndex)
        }
    }
}
