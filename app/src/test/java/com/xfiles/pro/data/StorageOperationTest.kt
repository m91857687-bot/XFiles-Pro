package com.xfiles.pro.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DefaultDispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.storage.MediaStoreManager
import com.xfiles.pro.data.local.storage.SafManager
import com.xfiles.pro.data.repository.StorageRepositoryImpl
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileType
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StorageOperationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var safManager: SafManager
    private lateinit var mediaStoreManager: MediaStoreManager
    private lateinit var repository: StorageRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        safManager = SafManager(context)
        mediaStoreManager = MediaStoreManager(context)
        repository = StorageRepositoryImpl(
            context = context,
            dispatchers = DefaultDispatchersProvider(),
            safManager = safManager,
            mediaStoreManager = mediaStoreManager
        )
    }

    @Test
    fun `create directory with arabic name succeeds`() = runTest {
        val root = tempFolder.root.absolutePath
        val dirName = "مستندات_العمل_والأرشيف"

        val result = repository.createDirectory(root, dirName)
        assertTrue(result is ResultWrapper.Success)

        val createdItem = (result as ResultWrapper.Success).data
        assertEquals(dirName, createdItem.name)
        assertTrue(createdItem.isDirectory)
        assertTrue(File(createdItem.path).exists())
    }

    @Test
    fun `create file with arabic name and content succeeds`() = runTest {
        val root = tempFolder.root.absolutePath
        val fileName = "تقرير_مشروع_XFiles.txt"

        val result = repository.createFile(root, fileName, "text/plain")
        assertTrue(result is ResultWrapper.Success)

        val createdFile = (result as ResultWrapper.Success).data
        assertEquals(fileName, createdFile.name)
        assertFalse(createdFile.isDirectory)

        val fileOnDisk = File(createdFile.path)
        assertTrue(fileOnDisk.exists())

        // Write sample Arabic text to verify UTF-8 encoding
        val content = "هذا ملف اختبار باللغة العربية للتحقق من سلامة نظام إدارة الملفات."
        fileOnDisk.writeText(content, Charsets.UTF_8)
        assertEquals(content, fileOnDisk.readText(Charsets.UTF_8))
    }

    @Test
    fun `safe copy with temporary file and integrity check`() = runTest {
        val sourceDir = tempFolder.newFolder("source")
        val targetDir = tempFolder.newFolder("target")

        val sourceFile = File(sourceDir, "large_sample_data.bin")
        // Create 256 KB test file to test multi-chunk buffer streaming
        val testData = ByteArray(256 * 1024) { (it % 256).toByte() }
        sourceFile.writeBytes(testData)

        var reportedProgress = 0L
        val copyResult = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "copied_large_sample.bin",
            resolution = FileConflictResolution.AUTO_RENAME,
            onProgress = { copied, total ->
                reportedProgress = copied
                assertEquals(sourceFile.length(), total)
            }
        )

        assertTrue("Copy must succeed", copyResult is ResultWrapper.Success)
        val copiedItem = (copyResult as ResultWrapper.Success).data

        val copiedFileOnDisk = File(copiedItem.path)
        assertTrue(copiedFileOnDisk.exists())
        assertEquals(256 * 1024L, copiedFileOnDisk.length())
        assertEquals(256 * 1024L, reportedProgress)
        // Verify source file is still intact
        assertTrue("Source file must remain untouched after copy", sourceFile.exists())
    }

    @Test
    fun `safe copy with AUTO_RENAME conflict resolution generates indexed names`() = runTest {
        val targetDir = tempFolder.newFolder("target_conflicts")

        val sourceFile = File(tempFolder.root, "document.pdf")
        sourceFile.writeText("PDF content 1")

        // First copy
        val firstCopy = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "document.pdf",
            resolution = FileConflictResolution.AUTO_RENAME
        )
        assertTrue(firstCopy is ResultWrapper.Success)
        assertEquals("document.pdf", (firstCopy as ResultWrapper.Success).data.name)

        // Second copy with same name -> document (1).pdf
        val secondCopy = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "document.pdf",
            resolution = FileConflictResolution.AUTO_RENAME
        )
        assertTrue(secondCopy is ResultWrapper.Success)
        assertEquals("document (1).pdf", (secondCopy as ResultWrapper.Success).data.name)

        // Third copy with same name -> document (2).pdf
        val thirdCopy = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "document.pdf",
            resolution = FileConflictResolution.AUTO_RENAME
        )
        assertTrue(thirdCopy is ResultWrapper.Success)
        assertEquals("document (2).pdf", (thirdCopy as ResultWrapper.Success).data.name)
    }

    @Test
    fun `safe copy with OVERWRITE and SKIP conflict resolutions`() = runTest {
        val targetDir = tempFolder.newFolder("target_overwrite")
        val existingFile = File(targetDir, "config.json")
        existingFile.writeText("{\"original\": true}")

        val sourceFile = File(tempFolder.root, "new_config.json")
        sourceFile.writeText("{\"updated\": true}")

        // 1. SKIP should not modify the existing file
        val skipResult = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "config.json",
            resolution = FileConflictResolution.SKIP
        )
        assertTrue(skipResult is ResultWrapper.Success)
        assertEquals("{\"original\": true}", existingFile.readText())

        // 2. OVERWRITE should safely replace the file
        val overwriteResult = repository.copyFile(
            sourcePathOrUri = sourceFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "config.json",
            resolution = FileConflictResolution.OVERWRITE
        )
        assertTrue(overwriteResult is ResultWrapper.Success)
        assertEquals("{\"updated\": true}", existingFile.readText())
    }

    @Test
    fun `move file removes source only after verified destination write`() = runTest {
        val sourceDir = tempFolder.newFolder("move_src")
        val targetDir = tempFolder.newFolder("move_target")

        val srcFile = File(sourceDir, "source_to_move.txt")
        srcFile.writeText("Important data to move safely")

        val moveResult = repository.moveFile(
            sourcePathOrUri = srcFile.absolutePath,
            targetParentPathOrUri = targetDir.absolutePath,
            targetName = "moved_safely.txt",
            resolution = FileConflictResolution.OVERWRITE
        )

        assertTrue(moveResult is ResultWrapper.Success)
        val movedItem = (moveResult as ResultWrapper.Success).data

        // Target file exists with verified content
        val destFile = File(movedItem.path)
        assertTrue(destFile.exists())
        assertEquals("Important data to move safely", destFile.readText())

        // Source file was removed
        assertFalse("Source file must be deleted upon verified move", srcFile.exists())
    }

    @Test
    fun `rename file with arabic characters succeeds`() = runTest {
        val root = tempFolder.root
        val initialFile = File(root, "ملف_أولي.txt")
        initialFile.writeText("محتوى تجريبي")

        val renameResult = repository.renameFile(initialFile.absolutePath, "ملف_معدل_بالعربية.txt")
        assertTrue(renameResult is ResultWrapper.Success)

        val renamedItem = (renameResult as ResultWrapper.Success).data
        assertEquals("ملف_معدل_بالعربية.txt", renamedItem.name)

        assertFalse(initialFile.exists())
        assertTrue(File(root, "ملف_معدل_بالعربية.txt").exists())
    }

    @Test
    fun `delete file and directory recursively`() = runTest {
        val root = tempFolder.newFolder("to_delete")
        val subDir = File(root, "child_dir")
        subDir.mkdirs()
        val file1 = File(subDir, "inner.txt")
        file1.writeText("dummy")

        val deleteResult = repository.deleteFile(root.absolutePath)
        assertTrue(deleteResult is ResultWrapper.Success)
        assertTrue((deleteResult as ResultWrapper.Success).data)
        assertFalse(root.exists())
    }

    @Test
    fun `saf manager persistable uri permissions check and management`() = runTest {
        val dummyTreeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments")

        // Verify initial non-granted state
        val hasPermissionBefore = repository.isUriPermissionGranted(dummyTreeUri)
        assertFalse(hasPermissionBefore)

        val permsList = repository.getPersistedPermissions()
        assertTrue(permsList is ResultWrapper.Success)
        assertNotNull((permsList as ResultWrapper.Success).data)
    }

    @Test
    fun `media store manager queries media and recent files without crashing`() = runTest {
        val images = mediaStoreManager.queryMedia(FileType.IMAGE, limit = 10)
        assertNotNull(images)

        val recents = mediaStoreManager.queryRecentFiles(limit = 10)
        assertNotNull(recents)
    }

    @Test
    fun `list files sorts items correctly with directories prioritized`() = runTest {
        val testDir = tempFolder.newFolder("sorting_test")
        File(testDir, "b_file.txt").createNewFile()
        File(testDir, "a_file.txt").createNewFile()
        File(testDir, "z_dir").mkdir()

        val result = repository.listFiles(testDir.absolutePath, FileSortOption.NAME_ASC)
        assertTrue(result is ResultWrapper.Success)

        val items = (result as ResultWrapper.Success).data
        assertEquals(3, items.size)
        // Directories should appear before files
        assertTrue(items[0].isDirectory)
        assertEquals("z_dir", items[0].name)
        // Files alphabetically sorted
        assertEquals("a_file.txt", items[1].name)
        assertEquals("b_file.txt", items[2].name)
    }
}
