package com.xfiles.pro.ui

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.DownloadItem
import com.xfiles.pro.domain.model.DownloadRequest
import com.xfiles.pro.domain.model.DownloadStatus
import com.xfiles.pro.domain.repository.DownloadRepository
import com.xfiles.pro.ui.download.DownloadFilter
import com.xfiles.pro.ui.download.DownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeDownloadsFlow = MutableStateFlow<List<DownloadItem>>(emptyList())
    private val pausedIds = mutableListOf<String>()
    private val resumedIds = mutableListOf<String>()
    private val cancelledIds = mutableListOf<String>()
    private val deletedIds = mutableListOf<String>()
    private var clearedCompleted = false

    private val fakeRepository = object : DownloadRepository {
        override fun getAllDownloads(): Flow<List<DownloadItem>> = fakeDownloadsFlow.asStateFlow()
        override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> = MutableStateFlow(emptyList())
        override fun getDownloadById(id: String): Flow<DownloadItem?> = MutableStateFlow(null)
        override suspend fun enqueueDownload(request: DownloadRequest): ResultWrapper<String> {
            val item = DownloadItem(
                id = "task_new",
                url = request.url,
                fileName = request.fileName ?: "file.ext",
                destinationDirectory = request.destinationDirectory,
                destinationFilePath = "${request.destinationDirectory}/${request.fileName ?: "file.ext"}",
                status = DownloadStatus.QUEUED
            )
            fakeDownloadsFlow.value = fakeDownloadsFlow.value + item
            return ResultWrapper.Success(item.id)
        }
        override suspend fun pauseDownload(id: String): ResultWrapper<Unit> {
            pausedIds.add(id)
            return ResultWrapper.Success(Unit)
        }
        override suspend fun resumeDownload(id: String): ResultWrapper<Unit> {
            resumedIds.add(id)
            return ResultWrapper.Success(Unit)
        }
        override suspend fun cancelDownload(id: String): ResultWrapper<Unit> {
            cancelledIds.add(id)
            return ResultWrapper.Success(Unit)
        }
        override suspend fun retryDownload(id: String): ResultWrapper<Unit> {
            resumedIds.add(id)
            return ResultWrapper.Success(Unit)
        }
        override suspend fun deleteDownload(id: String, deleteFile: Boolean): ResultWrapper<Unit> {
            deletedIds.add(id)
            fakeDownloadsFlow.value = fakeDownloadsFlow.value.filter { it.id != id }
            return ResultWrapper.Success(Unit)
        }
        override suspend fun renameCompletedDownload(id: String, newName: String): ResultWrapper<Unit> {
            fakeDownloadsFlow.value = fakeDownloadsFlow.value.map {
                if (it.id == id) it.copy(fileName = newName) else it
            }
            return ResultWrapper.Success(Unit)
        }
        override suspend fun clearCompletedDownloads(): ResultWrapper<Unit> {
            clearedCompleted = true
            fakeDownloadsFlow.value = fakeDownloadsFlow.value.filter { it.status != DownloadStatus.COMPLETED }
            return ResultWrapper.Success(Unit)
        }
        override fun getActiveDownloadsCount(): Flow<Int> = MutableStateFlow(0)
    }

    private lateinit var viewModel: DownloadViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDownloadsFlow.value = listOf(
            DownloadItem("1", "https://example.com/movie.mp4", "movie.mp4", "/downloads", "/downloads/movie.mp4", status = DownloadStatus.DOWNLOADING),
            DownloadItem("2", "https://example.com/song.mp3", "song.mp3", "/downloads", "/downloads/song.mp3", status = DownloadStatus.PAUSED),
            DownloadItem("3", "https://example.com/book.pdf", "book.pdf", "/downloads", "/downloads/book.pdf", status = DownloadStatus.COMPLETED)
        )
        viewModel = DownloadViewModel(
            downloadRepository = fakeRepository,
            defaultDownloadDir = "/downloads"
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects all items and default filter`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(3, state.items.size)
        assertEquals(3, state.filteredItems.size)
        assertEquals(DownloadFilter.ALL, state.filter)
        assertEquals(1, state.activeCount)
        assertEquals(1, state.completedCount)
    }

    @Test
    fun `switching filter filters displayed items correctly`() = runTest {
        viewModel.setFilter(DownloadFilter.ACTIVE)
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("movie.mp4", viewModel.uiState.value.filteredItems.first().fileName)

        viewModel.setFilter(DownloadFilter.COMPLETED)
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("book.pdf", viewModel.uiState.value.filteredItems.first().fileName)

        viewModel.setFilter(DownloadFilter.PAUSED_FAILED)
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("song.mp3", viewModel.uiState.value.filteredItems.first().fileName)
    }

    @Test
    fun `search query filters items by filename or url`() = runTest {
        viewModel.setSearchQuery("movie")
        assertEquals(1, viewModel.uiState.value.filteredItems.size)
        assertEquals("1", viewModel.uiState.value.filteredItems.first().id)

        viewModel.setSearchQuery("nonexistent")
        assertTrue(viewModel.uiState.value.filteredItems.isEmpty())

        viewModel.setSearchQuery("")
        assertEquals(3, viewModel.uiState.value.filteredItems.size)
    }

    @Test
    fun `dialog open and close controls operate cleanly`() = runTest {
        assertFalse(viewModel.uiState.value.showNewDownloadDialog)
        viewModel.openNewDownloadDialog()
        assertTrue(viewModel.uiState.value.showNewDownloadDialog)
        viewModel.closeNewDownloadDialog()
        assertFalse(viewModel.uiState.value.showNewDownloadDialog)

        val item = viewModel.uiState.value.items.first()
        viewModel.openDetails(item)
        assertTrue(viewModel.uiState.value.showDetailsDialog)
        assertEquals("1", viewModel.uiState.value.selectedItem?.id)
        viewModel.closeDetails()
        assertFalse(viewModel.uiState.value.showDetailsDialog)
        assertNull(viewModel.uiState.value.selectedItem)
    }

    @Test
    fun `starting download enqueues task and updates state`() = runTest {
        viewModel.openNewDownloadDialog()
        viewModel.startDownload("https://example.com/new_archive.zip", "new_archive.zip", "/downloads")

        val state = viewModel.uiState.value
        assertFalse(state.showNewDownloadDialog)
        assertEquals(4, state.items.size)
        assertTrue(state.items.any { it.fileName == "new_archive.zip" })
    }

    @Test
    fun `pause, resume, and cancel calls delegate to repository`() = runTest {
        viewModel.pauseDownload("1")
        assertTrue(pausedIds.contains("1"))

        viewModel.resumeDownload("2")
        assertTrue(resumedIds.contains("2"))

        viewModel.cancelDownload("1")
        assertTrue(cancelledIds.contains("1"))
    }

    @Test
    fun `delete download and clear completed downloads update state`() = runTest {
        viewModel.confirmDelete("3", deleteFile = false)
        assertTrue(deletedIds.contains("3"))
        assertEquals(2, viewModel.uiState.value.items.size)

        viewModel.clearCompleted()
        assertTrue(clearedCompleted)
    }
}
