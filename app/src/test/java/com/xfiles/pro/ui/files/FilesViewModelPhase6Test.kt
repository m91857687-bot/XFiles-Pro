package com.xfiles.pro.ui.files

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.database.XFilesDatabase
import com.xfiles.pro.data.local.datastore.SearchPreferences
import com.xfiles.pro.data.local.datastore.TabPreferences
import com.xfiles.pro.data.local.datastore.dataStore
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.data.repository.FileIndexingRepositoryImpl
import com.xfiles.pro.data.repository.SearchHistoryRepositoryImpl
import com.xfiles.pro.data.repository.StorageRepositoryImpl
import com.xfiles.pro.data.repository.TabRepositoryImpl
import com.xfiles.pro.domain.model.DateFilterRange
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.domain.model.SearchTarget
import com.xfiles.pro.domain.model.SizeFilterRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FilesViewModelPhase6Test {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var testScope: CoroutineScope

    private lateinit var context: Context
    private lateinit var database: XFilesDatabase
    private lateinit var storageRepository: StorageRepositoryImpl
    private lateinit var indexingRepository: FileIndexingRepositoryImpl
    private lateinit var tabRepository: TabRepositoryImpl
    private lateinit var searchHistoryRepository: SearchHistoryRepositoryImpl
    private lateinit var viewModel: FilesViewModel

    private lateinit var rootDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        kotlinx.coroutines.runBlocking {
            context.dataStore.edit { it.clear() }
        }

        rootDir = tempFolder.newFolder()

        val dispatchers = object : com.xfiles.pro.core.dispatcher.DispatchersProvider {
            override val main: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val io: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val default: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val unconfined: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
        }
        database = Room.inMemoryDatabaseBuilder(context, XFilesDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val safManager = SafManager(context)
        val mediaStoreManager = MediaStoreManager(context)

        storageRepository = StorageRepositoryImpl(context, dispatchers, safManager, mediaStoreManager)
        indexingRepository = FileIndexingRepositoryImpl(
            context = context,
            dispatchers = dispatchers,
            indexedFileDao = database.indexedFileDao(),
            storageRepository = storageRepository,
            mediaStoreManager = mediaStoreManager,
            safManager = safManager,
            externalScope = testScope
        )
        val tabPreferences = TabPreferences(context)
        tabRepository = TabRepositoryImpl(tabPreferences, dispatchers)

        val searchPreferences = SearchPreferences(context)
        searchHistoryRepository = SearchHistoryRepositoryImpl(searchPreferences, dispatchers)

        viewModel = FilesViewModel(
            storageRepository = storageRepository,
            fileIndexingRepository = indexingRepository,
            tabRepository = tabRepository,
            searchHistoryRepository = searchHistoryRepository
        )
    }

    @After
    fun tearDown() {
        if (::testScope.isInitialized) {
            testScope.cancel()
        }
        if (::database.isInitialized) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts with at least one tab`() = runTest {
        val state = viewModel.uiState.value
        assertTrue("Tabs must not be empty", state.tabs.isNotEmpty())
        assertEquals(0, state.activeTabIndex)
        assertNotNull(state.activeTab)
    }

    @Test
    fun `opening new tab adds to list and switches active tab`() = runTest {
        val folder = File(rootDir, "Music").apply { mkdirs() }
        viewModel.openNewTab(folder.absolutePath, "Music")

        val state = viewModel.uiState.value
        assertEquals(2, state.tabs.size)
        assertEquals(1, state.activeTabIndex)
        assertEquals("Music", state.activeTab?.title)
        assertEquals(folder.absolutePath, state.activeTab?.path)
    }

    @Test
    fun `closing a tab does not allow closing the last tab`() = runTest {
        // Initial tabs count: 1
        assertEquals(1, viewModel.uiState.value.tabs.size)

        // Try closing the only tab
        viewModel.closeTab(0)
        assertEquals(1, viewModel.uiState.value.tabs.size)

        // Open a second tab, now closing one should leave one
        val docs = File(rootDir, "Docs").apply { mkdirs() }
        viewModel.openNewTab(docs.absolutePath, "Docs")
        assertEquals(2, viewModel.uiState.value.tabs.size)

        viewModel.closeTab(1)
        assertEquals(1, viewModel.uiState.value.tabs.size)
    }

    @Test
    fun `close other tabs leaves only active tab`() = runTest {
        val f1 = File(rootDir, "F1").apply { mkdirs() }
        val f2 = File(rootDir, "F2").apply { mkdirs() }

        viewModel.openNewTab(f1.absolutePath, "F1")
        viewModel.openNewTab(f2.absolutePath, "F2")
        assertEquals(3, viewModel.uiState.value.tabs.size)

        viewModel.closeOtherTabs()
        assertEquals(1, viewModel.uiState.value.tabs.size)
        assertEquals("F2", viewModel.uiState.value.activeTab?.title)
    }

    @Test
    fun `dual pane toggle changes dual pane mode and active pane`() = runTest {
        assertFalse(viewModel.uiState.value.isDualPaneMode)
        viewModel.toggleDualPaneMode()
        assertTrue(viewModel.uiState.value.isDualPaneMode)

        assertEquals(PaneId.PRIMARY, viewModel.uiState.value.activePaneId)
        viewModel.setActivePane(PaneId.SECONDARY)
        assertEquals(PaneId.SECONDARY, viewModel.uiState.value.activePaneId)
    }

    @Test
    fun `copy and move file to other pane invokes storage operations`() = runTest {
        val primaryDir = File(rootDir, "primary").apply { mkdirs() }
        val secondaryDir = File(rootDir, "secondary").apply { mkdirs() }

        val sourceFile = File(primaryDir, "notes.txt").apply { writeText("Sample Content") }

        viewModel.loadDirectory(primaryDir.absolutePath, PaneId.PRIMARY)
        viewModel.loadDirectory(secondaryDir.absolutePath, PaneId.SECONDARY)
        viewModel.setActivePane(PaneId.PRIMARY)

        val fileItem = FileItem(
            name = "notes.txt",
            path = sourceFile.absolutePath,
            size = sourceFile.length(),
            lastModified = sourceFile.lastModified(),
            isDirectory = false,
            extension = "txt"
        )

        // Copy to other pane
        viewModel.copyFileToOtherPane(fileItem)

        // Target file should now exist in secondaryDir
        val copiedFile = File(secondaryDir, "notes.txt")
        assertTrue("Copied file must exist in secondary pane folder", copiedFile.exists())
        assertEquals("Sample Content", copiedFile.readText())
    }

    @Test
    fun `advanced search criteria updates and clear filters works`() = runTest {
        viewModel.onSearchQueryChange("report")
        viewModel.setFileTypeFilter(FileType.DOCUMENT)
        viewModel.setSizeFilter(SizeFilterRange.MEDIUM_1_50MB)
        viewModel.setDateFilter(DateFilterRange.THIS_WEEK)
        viewModel.setSearchTarget(SearchTarget.FILES_ONLY)

        val criteria = viewModel.uiState.value.searchCriteria
        assertEquals("report", criteria.query)
        assertEquals(FileType.DOCUMENT, criteria.fileType)
        assertEquals(SizeFilterRange.MEDIUM_1_50MB, criteria.sizeRange)
        assertEquals(DateFilterRange.THIS_WEEK, criteria.dateRange)
        assertEquals(SearchTarget.FILES_ONLY, criteria.searchTarget)
        assertTrue(criteria.isFilteringActive)

        viewModel.clearFilters()
        val cleared = viewModel.uiState.value.searchCriteria
        assertEquals("report", cleared.query) // Query preserved
        assertFalse(cleared.isFilteringActive) // Filters reset
    }

    @Test
    fun `open search result in new tab adds tab and closes search`() = runTest {
        val targetFolder = File(rootDir, "TargetFolder").apply { mkdirs() }
        val searchItem = FileItem(
            name = "TargetFolder",
            path = targetFolder.absolutePath,
            size = 0L,
            lastModified = 0L,
            isDirectory = true
        )

        viewModel.onSearchQueryChange("Target")
        assertTrue(viewModel.uiState.value.isSearching)

        viewModel.openSearchResultInNewTab(searchItem)

        assertFalse(viewModel.uiState.value.isSearching)
        assertEquals("TargetFolder", viewModel.uiState.value.activeTab?.title)
    }

    @Test
    fun `navigate to parent folder from search result updates active pane`() = runTest {
        val parentDir = File(rootDir, "ParentFolder").apply { mkdirs() }
        val childFile = File(parentDir, "item.txt").apply { writeText("abc") }

        val searchItem = FileItem(
            name = "item.txt",
            path = childFile.absolutePath,
            size = 3L,
            lastModified = 0L,
            isDirectory = false
        )

        viewModel.onSearchQueryChange("item")
        assertTrue(viewModel.uiState.value.isSearching)

        viewModel.navigateToParentFolder(searchItem)

        assertFalse(viewModel.uiState.value.isSearching)
        assertEquals(parentDir.absolutePath, viewModel.uiState.value.activePaneState.currentPath)
    }
}
