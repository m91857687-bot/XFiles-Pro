package com.xfiles.pro.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.datastore.SearchPreferences
import com.xfiles.pro.data.local.datastore.dataStore
import com.xfiles.pro.data.repository.SearchHistoryRepositoryImpl
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
class SearchPreferencesTest {

    private lateinit var context: Context
    private lateinit var searchPreferences: SearchPreferences
    private lateinit var searchHistoryRepository: SearchHistoryRepositoryImpl

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }

        val dispatchers = DefaultDispatchersProvider()
        searchPreferences = SearchPreferences(context)
        searchHistoryRepository = SearchHistoryRepositoryImpl(searchPreferences, dispatchers)
    }

    @Test
    fun `initial search history is empty`() = runTest {
        val history = searchHistoryRepository.recentSearches.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `adding search queries preserves recent order and moves duplicates to top`() = runTest {
        searchHistoryRepository.addSearchQuery("PDF")
        searchHistoryRepository.addSearchQuery("invoice")
        searchHistoryRepository.addSearchQuery("photo")

        var history = searchHistoryRepository.recentSearches.first()
        assertEquals(listOf("photo", "invoice", "PDF"), history)

        // Adding "PDF" again should bring it to the top without duplication
        searchHistoryRepository.addSearchQuery("pdf")
        history = searchHistoryRepository.recentSearches.first()
        assertEquals(listOf("pdf", "photo", "invoice"), history)
    }

    @Test
    fun `removing individual query and clearing history works`() = runTest {
        searchHistoryRepository.addSearchQuery("android")
        searchHistoryRepository.addSearchQuery("backup")
        searchHistoryRepository.addSearchQuery("music")

        searchHistoryRepository.removeSearchQuery("backup")
        var history = searchHistoryRepository.recentSearches.first()
        assertEquals(listOf("music", "android"), history)

        searchHistoryRepository.clearHistory()
        history = searchHistoryRepository.recentSearches.first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `history is limited to max 15 items`() = runTest {
        for (i in 1..20) {
            searchHistoryRepository.addSearchQuery("query_$i")
        }

        val history = searchHistoryRepository.recentSearches.first()
        assertEquals(15, history.size)
        assertEquals("query_20", history.first())
        assertEquals("query_6", history.last())
    }
}
