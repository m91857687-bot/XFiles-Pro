package com.xfiles.pro.data

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.archive.SevenZArchiveHandler
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SevenZArchiveHandlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var handler: SevenZArchiveHandler

    @Before
    fun setup() {
        handler = SevenZArchiveHandler()
    }

    @Test
    fun createAndExtract7z_success() = runBlocking {
        val srcDir = tempFolder.newFolder("src_7z")
        val file1 = File(srcDir, "sample.txt").apply { writeText("7-Zip compression content") }
        val file2 = File(srcDir, "arabic_data.txt").apply { writeText("محتوى ملف مضغوط بسبعة زيب") }

        val dest7z = File(tempFolder.root, "archive.7z")
        val createResult = handler.createArchive(
            sourceFiles = listOf(file1, file2),
            destinationFile = dest7z,
            compressionLevel = CompressionLevel.NORMAL
        )

        assertTrue("Creation must succeed", createResult is ResultWrapper.Success)
        assertTrue("7z file must exist", dest7z.exists())

        // List entries
        val listResult = handler.listEntries(dest7z)
        assertTrue("Listing must succeed", listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.name == "sample.txt" })
        assertTrue(entries.any { it.name == "arabic_data.txt" })

        // Extract
        val outDir = tempFolder.newFolder("out_7z")
        val extractResult = handler.extractArchive(
            archiveFile = dest7z,
            destinationDir = outDir,
            conflictResolution = FileConflictResolution.OVERWRITE
        )
        assertTrue("Extraction must succeed", extractResult is ResultWrapper.Success)

        val restored1 = File(outDir, "sample.txt")
        val restored2 = File(outDir, "arabic_data.txt")
        assertTrue("Restored file 1 must exist", restored1.exists())
        assertTrue("Restored file 2 must exist", restored2.exists())
        assertEquals("7-Zip compression content", restored1.readText())
        assertEquals("محتوى ملف مضغوط بسبعة زيب", restored2.readText())
    }
}
