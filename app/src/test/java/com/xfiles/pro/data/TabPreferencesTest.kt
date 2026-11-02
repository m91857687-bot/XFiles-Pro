package com.xfiles.pro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.datastore.TabPreferences
import com.xfiles.pro.data.local.datastore.dataStore
import com.xfiles.pro.data.repository.TabRepositoryImpl
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TabPreferencesTest {

    private lateinit var context: Context
    private lateinit var tabPreferences: TabPreferences
    private lateinit var tabRepository: TabRepositoryImpl

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }

        val dispatchers = DefaultDispatchersProvider()
        tabPreferences = TabPreferences(context)
        tabRepository = TabRepositoryImpl(tabPreferences, dispatchers)
    }

    @Test
    fun `empty initial state returns empty tabs and default index 0`() = runTest {
        val tabs = tabRepository.savedTabs.first()
        val index = tabRepository.activeTabIndex.first()

        assertTrue(tabs.isEmpty())
        assertEquals(0, index)
    }

    @Test
    fun `save and restore multiple tabs with custom view mode and sort options`() = runTest {
        val originalTabs = listOf(
            FileTab(
                id = "tab-1",
                title = "Downloads",
                path = "/storage/0/Download",
                isGridView = true,
                sortOption = FileSortOption.DATE_DESC
            ),
            FileTab(
                id = "tab-2",
                title = "DCIM",
                path = "/storage/0/DCIM",
                isGridView = false,
                sortOption = FileSortOption.SIZE_DESC
            )
        )

        tabRepository.saveTabs(originalTabs, activeIndex = 1)

        val restoredTabs = tabRepository.savedTabs.first()
        val restoredIndex = tabRepository.activeTabIndex.first()

        assertEquals(2, restoredTabs.size)
        assertEquals(1, restoredIndex)

        assertEquals("tab-1", restoredTabs[0].id)
        assertEquals("Downloads", restoredTabs[0].title)
        assertEquals("/storage/0/Download", restoredTabs[0].path)
        assertTrue(restoredTabs[0].isGridView)
        assertEquals(FileSortOption.DATE_DESC, restoredTabs[0].sortOption)

        assertEquals("tab-2", restoredTabs[1].id)
        assertEquals("DCIM", restoredTabs[1].title)
        assertEquals("/storage/0/DCIM", restoredTabs[1].path)
        assertEquals(false, restoredTabs[1].isGridView)
        assertEquals(FileSortOption.SIZE_DESC, restoredTabs[1].sortOption)
    }

    @Test
    fun `active index is safely coerced within bounds`() = runTest {
        val tabs = listOf(
            FileTab(title = "Root", path = "/storage/0")
        )
        // Attempt saving out-of-bounds active index 5
        tabRepository.saveTabs(tabs, activeIndex = 5)

        val restoredIndex = tabRepository.activeTabIndex.first()
        assertEquals(0, restoredIndex)
    }
}
