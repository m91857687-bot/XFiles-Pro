package com.xfiles.pro.ui

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.repository.ArchiveRepository
import com.xfiles.pro.ui.archive.ArchiveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeArchiveRepository : ArchiveRepository {
        var isEncrypted = false
        var entries = listOf<ArchiveEntryItem>()
        var extractedCount = 0

        override suspend fun listEntries(archiveFile: File, password: String?): ResultWrapper<List<ArchiveEntryItem>> {
            return if (isEncrypted && password.isNullOrEmpty()) {
                ResultWrapper.Error(IllegalStateException("Password required"), "Password required")
            } else {
                ResultWrapper.Success(entries)
            }
        }

        override suspend fun extractArchive(
            archiveFile: File,
            destinationDir: File,
            password: String?,
            selectedEntries: List<String>?,
            conflictResolution: FileConflictResolution,
            onProgress: (ArchiveProgress) -> Unit
        ): ResultWrapper<Int> {
            onProgress(ArchiveProgress(currentItem = "file.txt", percentage = 1.0f))
            return ResultWrapper.Success(extractedCount)
        }

        override suspend fun createArchive(
            sourceFiles: List<File>,
            destinationArchiveFile: File,
            format: ArchiveFormat,
            compressionLevel: CompressionLevel,
            password: String?,
            onProgress: (ArchiveProgress) -> Unit
        ): ResultWrapper<File> {
            return ResultWrapper.Success(destinationArchiveFile)
        }

        override suspend fun deleteEntriesFromZip(
            zipFile: File,
            entryPathsToDelete: List<String>
        ): ResultWrapper<Boolean> {
            return ResultWrapper.Success(true)
        }

        override suspend fun addFilesToZip(
            zipFile: File,
            filesToAdd: List<File>,
            targetDirectoryInsideArchive: String,
            onProgress: (ArchiveProgress) -> Unit
        ): ResultWrapper<Boolean> {
            return ResultWrapper.Success(true)
        }

        override suspend fun isPasswordProtected(archiveFile: File): Boolean = isEncrypted
    }

    private lateinit var fakeRepo: FakeArchiveRepository
    private lateinit var viewModel: ArchiveViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeArchiveRepository()
        viewModel = ArchiveViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadArchive_unencrypted_displaysRootVirtualEntries() = runTest {
        val archiveFile = File(tempFolder.root, "sample.zip")
        fakeRepo.entries = listOf(
            ArchiveEntryItem(path = "readme.txt", name = "readme.txt", isDirectory = false, uncompressedSize = 100L, compressedSize = 50L),
            ArchiveEntryItem(path = "docs/guide.pdf", name = "guide.pdf", isDirectory = false, uncompressedSize = 500L, compressedSize = 250L)
        )

        viewModel.loadArchive(archiveFile)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.allEntries.size)
        // At root level, "docs" should be synthesized as a virtual directory and "readme.txt" as a file
        assertEquals(2, state.visibleEntries.size)
        assertTrue(state.visibleEntries.any { it.name == "readme.txt" && !it.isDirectory })
        assertTrue(state.visibleEntries.any { it.name == "docs" && it.isDirectory })
        assertEquals(600L, state.totalUncompressedSize)
    }

    @Test
    fun virtualFolderNavigation_enterAndNavigateUp_worksCorrectly() = runTest {
        val archiveFile = File(tempFolder.root, "test.zip")
        fakeRepo.entries = listOf(
            ArchiveEntryItem(path = "root.txt", name = "root.txt", isDirectory = false),
            ArchiveEntryItem(path = "sub/file1.txt", name = "file1.txt", isDirectory = false),
            ArchiveEntryItem(path = "sub/nested/file2.txt", name = "file2.txt", isDirectory = false)
        )

        viewModel.loadArchive(archiveFile)

        // Enter "sub"
        viewModel.enterVirtualFolder("sub")
        var state = viewModel.uiState.value
        assertEquals("sub", state.currentVirtualPath)
        assertEquals(2, state.visibleEntries.size)
        assertTrue(state.visibleEntries.any { it.name == "file1.txt" && !it.isDirectory })
        assertTrue(state.visibleEntries.any { it.name == "nested" && it.isDirectory })

        // Navigate Up
        val handled = viewModel.navigateUp()
        assertTrue(handled)
        state = viewModel.uiState.value
        assertEquals("", state.currentVirtualPath)
    }

    @Test
    fun searchInsideArchive_filtersMatchingEntries() = runTest {
        val archiveFile = File(tempFolder.root, "search.zip")
        fakeRepo.entries = listOf(
            ArchiveEntryItem(path = "report_2026.pdf", name = "report_2026.pdf", isDirectory = false),
            ArchiveEntryItem(path = "images/photo.png", name = "photo.png", isDirectory = false)
        )

        viewModel.loadArchive(archiveFile)

        viewModel.setSearchQuery("photo")
        val state = viewModel.uiState.value
        assertEquals(1, state.visibleEntries.size)
        assertEquals("photo.png", state.visibleEntries.first().name)

        viewModel.setSearchQuery("")
        assertEquals(2, viewModel.uiState.value.visibleEntries.size)
    }

    @Test
    fun selectionManagement_toggleSelectAllAndClear_success() = runTest {
        val archiveFile = File(tempFolder.root, "sel.zip")
        fakeRepo.entries = listOf(
            ArchiveEntryItem(path = "a.txt", name = "a.txt", isDirectory = false),
            ArchiveEntryItem(path = "b.txt", name = "b.txt", isDirectory = false)
        )
        viewModel.loadArchive(archiveFile)

        viewModel.toggleSelection("a.txt")
        assertTrue(viewModel.uiState.value.selectedEntryPaths.contains("a.txt"))

        viewModel.selectAll()
        assertEquals(2, viewModel.uiState.value.selectedEntryPaths.size)

        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selectedEntryPaths.isEmpty())
    }

    @Test
    fun encryptedArchive_promptsForPassword() = runTest {
        val archiveFile = File(tempFolder.root, "secret.zip")
        fakeRepo.isEncrypted = true

        viewModel.loadArchive(archiveFile)

        val state = viewModel.uiState.value
        assertTrue(state.showPasswordPrompt)
        assertTrue(state.isEncrypted)
    }

    @Test
    fun extractOperation_updatesStateAndReportsSuccess() = runTest {
        val archiveFile = File(tempFolder.root, "extract.zip")
        fakeRepo.extractedCount = 5
        fakeRepo.entries = listOf(ArchiveEntryItem(path = "f.txt", name = "f.txt", isDirectory = false))
        viewModel.loadArchive(archiveFile)

        val dest = File(tempFolder.root, "out")
        viewModel.extract(dest)

        val state = viewModel.uiState.value
        assertFalse(state.isOperationInProgress)
        assertNotNull(state.operationSuccessMessage)
        assertTrue(state.operationSuccessMessage?.contains("5 files") == true)
    }
}
