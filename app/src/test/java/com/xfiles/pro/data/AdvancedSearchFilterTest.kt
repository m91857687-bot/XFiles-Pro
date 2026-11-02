package com.xfiles.pro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.data.local.database.XFilesDatabase
import com.xfiles.pro.data.local.database.dao.IndexedFileDao
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.data.repository.FileIndexingRepositoryImpl
import com.xfiles.pro.data.repository.StorageRepositoryImpl
import com.xfiles.pro.domain.model.DateFilterRange
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.SearchFilterCriteria
import com.xfiles.pro.domain.model.SearchTarget
import com.xfiles.pro.domain.model.SizeFilterRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdvancedSearchFilterTest {

    private lateinit var context: Context
    private lateinit var database: XFilesDatabase
    private lateinit var dao: IndexedFileDao
    private lateinit var storageRepository: StorageRepositoryImpl
    private lateinit var indexingRepository: FileIndexingRepositoryImpl

    private val now = System.currentTimeMillis()
    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var dispatchers: com.xfiles.pro.core.dispatcher.DispatchersProvider

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        dispatchers = object : com.xfiles.pro.core.dispatcher.DispatchersProvider {
            override val main: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val io: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val default: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
            override val unconfined: kotlinx.coroutines.CoroutineDispatcher = testDispatcher
        }
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, XFilesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.indexedFileDao()

        val safManager = SafManager(context)
        val mediaStoreManager = MediaStoreManager(context)

        storageRepository = StorageRepositoryImpl(context, dispatchers, safManager, mediaStoreManager)
        indexingRepository = FileIndexingRepositoryImpl(
            context = context,
            dispatchers = dispatchers,
            indexedFileDao = dao,
            storageRepository = storageRepository,
            mediaStoreManager = mediaStoreManager,
            safManager = safManager
        )

        // Seed test data with diverse sizes, categories, dates, and names
        val entities = listOf(
            IndexedFileEntity(
                name = "financial_report_2026.pdf",
                path = "/storage/0/Documents/financial_report_2026.pdf",
                parentPath = "/storage/0/Documents",
                size = 500 * 1024L, // 500 KB (Tiny)
                lastModified = now - TimeUnit.HOURS.toMillis(2), // Today
                isDirectory = false,
                extension = "pdf",
                mimeType = "application/pdf",
                category = "DOCUMENT"
            ),
            IndexedFileEntity(
                name = "تقرير_الميزانية_المفصل.pdf",
                path = "/storage/0/Documents/تقرير_الميزانية_المفصل.pdf",
                parentPath = "/storage/0/Documents",
                size = 5 * 1024 * 1024L, // 5 MB (Medium)
                lastModified = now - TimeUnit.DAYS.toMillis(3), // This week
                isDirectory = false,
                extension = "pdf",
                mimeType = "application/pdf",
                category = "DOCUMENT"
            ),
            IndexedFileEntity(
                name = "vacation_video.mp4",
                path = "/storage/0/Movies/vacation_video.mp4",
                parentPath = "/storage/0/Movies",
                size = 120 * 1024 * 1024L, // 120 MB (Large)
                lastModified = now - TimeUnit.DAYS.toMillis(15), // This month
                isDirectory = false,
                extension = "mp4",
                mimeType = "video/mp4",
                category = "VIDEO"
            ),
            IndexedFileEntity(
                name = "Documents",
                path = "/storage/0/Documents",
                parentPath = "/storage/0",
                size = 0L,
                lastModified = now - TimeUnit.HOURS.toMillis(5),
                isDirectory = true,
                extension = "",
                mimeType = "inode/directory",
                category = "OTHER"
            ),
            IndexedFileEntity(
                name = "database_backup.zip",
                path = "/storage/0/Backups/database_backup.zip",
                parentPath = "/storage/0/Backups",
                size = 600 * 1024 * 1024L, // 600 MB (Huge)
                lastModified = now - TimeUnit.DAYS.toMillis(100), // This year
                isDirectory = false,
                extension = "zip",
                mimeType = "application/zip",
                category = "ARCHIVE"
            )
        )

        kotlinx.coroutines.runBlocking {
            dao.insertAll(entities)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `search with size filter filters out files outside range`() = runTest {
        // Search all documents with Size Tiny (< 1MB)
        val criteria = SearchFilterCriteria(
            query = "report",
            sizeRange = SizeFilterRange.TINY_UNDER_1MB
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(1, results.size)
        assertEquals("financial_report_2026.pdf", results[0].name)
    }

    @Test
    fun `search with arabic query and category filter returns exact match`() = runTest {
        val criteria = SearchFilterCriteria(
            query = "الميزانية",
            fileType = FileType.DOCUMENT
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(1, results.size)
        assertEquals("تقرير_الميزانية_المفصل.pdf", results[0].name)
    }

    @Test
    fun `search target folders only returns directories and excludes files`() = runTest {
        val criteria = SearchFilterCriteria(
            query = "Documents",
            searchTarget = SearchTarget.FOLDERS_ONLY
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(1, results.size)
        assertTrue(results[0].isDirectory)
        assertEquals("Documents", results[0].name)
    }

    @Test
    fun `search target files only excludes matching folders`() = runTest {
        val criteria = SearchFilterCriteria(
            query = "report",
            searchTarget = SearchTarget.FILES_ONLY
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(1, results.size)
        assertEquals("financial_report_2026.pdf", results[0].name)
    }

    @Test
    fun `scope path restricts results to specific folder hierarchy`() = runTest {
        // Scope to /storage/0/Movies
        val criteria = SearchFilterCriteria(
            query = "video",
            scopePath = "/storage/0/Movies"
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(1, results.size)
        assertEquals("vacation_video.mp4", results[0].name)
    }

    @Test
    fun `search sorting applies correct ordering on results`() = runTest {
        val criteria = SearchFilterCriteria(
            query = "pdf",
            sortOption = FileSortOption.SIZE_DESC
        )

        val results = indexingRepository.searchFilesAdvanced(criteria).first()
        assertEquals(2, results.size)
        // 5MB should come before 500KB
        assertEquals("تقرير_الميزانية_المفصل.pdf", results[0].name)
        assertEquals("financial_report_2026.pdf", results[1].name)
    }
}
