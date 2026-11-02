package com.xfiles.pro.data

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.archive.ZipArchiveHandler
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipArchiveHandlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var handler: ZipArchiveHandler

    @Before
    fun setup() {
        handler = ZipArchiveHandler()
    }

    @Test
    fun createAndListZip_unencrypted_success() = runBlocking {
        val srcDir = tempFolder.newFolder("src")
        val file1 = File(srcDir, "doc.txt").apply { writeText("Hello World!") }
        val file2 = File(srcDir, "arabic.txt").apply { writeText("مرحبا بكم في XFiles Pro") }

        val zipFile = File(tempFolder.root, "test.zip")
        val createResult = handler.createArchive(
            sourceFiles = listOf(file1, file2),
            destinationArchiveFile = zipFile,
            compressionLevel = CompressionLevel.NORMAL
        )

        assertTrue(createResult is ResultWrapper.Success)
        assertTrue(zipFile.exists())
        assertFalse(handler.isEncrypted(zipFile))

        val listResult = handler.listEntries(zipFile)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.name == "doc.txt" })
        assertTrue(entries.any { it.name == "arabic.txt" })
    }

    @Test
    fun extractZip_withConflictAutoRename_createsDistinctFile() = runBlocking {
        val srcDir = tempFolder.newFolder("src_conflict")
        val file1 = File(srcDir, "data.txt").apply { writeText("Original content") }

        val zipFile = File(tempFolder.root, "conflict.zip")
        handler.createArchive(listOf(file1), zipFile)

        val outDir = tempFolder.newFolder("out_conflict")
        // Pre-create an existing file in outDir
        File(outDir, "data.txt").apply { writeText("Pre-existing content") }

        val extractResult = handler.extractArchive(
            archiveFile = zipFile,
            destinationDir = outDir,
            conflictResolution = FileConflictResolution.AUTO_RENAME
        )

        assertTrue(extractResult is ResultWrapper.Success)
        val extractedFiles = outDir.listFiles() ?: emptyArray()
        assertEquals(2, extractedFiles.size)
        assertTrue(extractedFiles.any { it.name == "data.txt" })
        assertTrue(extractedFiles.any { it.name == "data (1).txt" })
    }

    @Test
    fun passwordProtectedZip_requiresPasswordToExtract() = runBlocking {
        val srcDir = tempFolder.newFolder("src_secret")
        val secretFile = File(srcDir, "secret.txt").apply { writeText("Confidential 12345") }

        val zipFile = File(tempFolder.root, "encrypted.zip")
        val password = "StrongPassword#2026"

        val createResult = handler.createArchive(
            sourceFiles = listOf(secretFile),
            destinationArchiveFile = zipFile,
            password = password
        )
        assertTrue(createResult is ResultWrapper.Success)
        assertTrue(handler.isEncrypted(zipFile))

        val outDir = tempFolder.newFolder("out_secret")
        val extractWithPassword = handler.extractArchive(
            archiveFile = zipFile,
            destinationDir = outDir,
            password = password
        )
        assertTrue(extractWithPassword is ResultWrapper.Success)
        val decryptedFile = File(outDir, "secret.txt")
        assertTrue(decryptedFile.exists())
        assertEquals("Confidential 12345", decryptedFile.readText())
    }

    @Test
    fun deleteEntries_fromZip_removesSpecifiedFile() = runBlocking {
        val srcDir = tempFolder.newFolder("src_delete")
        val fileA = File(srcDir, "keep.txt").apply { writeText("Keep me") }
        val fileB = File(srcDir, "remove.txt").apply { writeText("Delete me") }

        val zipFile = File(tempFolder.root, "delete_test.zip")
        handler.createArchive(listOf(fileA, fileB), zipFile)

        val deleteResult = handler.deleteEntries(zipFile, listOf("remove.txt"))
        assertTrue(deleteResult is ResultWrapper.Success)

        val listResult = handler.listEntries(zipFile)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(1, entries.size)
        assertEquals("keep.txt", entries.first().name)
    }
}
