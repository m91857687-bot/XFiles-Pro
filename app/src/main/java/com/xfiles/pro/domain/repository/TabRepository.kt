package com.xfiles.pro.domain.repository

import com.xfiles.pro.domain.model.FileTab
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for persisting and restoring multi-tab browsing sessions.
 */
interface TabRepository {
    /**
     * Flow emitting the currently saved tabs across app launches.
     */
    val savedTabs: Flow<List<FileTab>>

    /**
     * Flow emitting the index of the active tab.
     */
    val activeTabIndex: Flow<Int>

    /**
     * Persist current tabs and active index to permanent storage.
     */
    suspend fun saveTabs(tabs: List<FileTab>, activeIndex: Int)
}
