package com.xfiles.pro.data

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.archive.TarArchiveHandler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TarArchiveHandlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var handler: TarArchiveHandler

    @Before
    fun setup() {
        handler = TarArchiveHandler()
    }

    @Test
    fun createAndExtractTar_plain_success() = runBlocking {
        val srcDir = tempFolder.newFolder("src_tar")
        val file1 = File(srcDir, "log.txt").apply { writeText("Sample log content") }

        val tarFile = File(tempFolder.root, "test.tar")
        val createResult = handler.createArchive(
            sourceFiles = listOf(file1),
            destinationFile = tarFile,
            isGzip = false
        )
        assertTrue(createResult is ResultWrapper.Success)
        assertTrue(tarFile.exists())

        val listResult = handler.listEntries(tarFile, isGzip = false)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(1, entries.size)
        assertEquals("log.txt", entries.first().name)

        val outDir = tempFolder.newFolder("out_tar")
        val extractResult = handler.extractArchive(
            archiveFile = tarFile,
            destinationDir = outDir,
            isGzip = false
        )
        assertTrue(extractResult is ResultWrapper.Success)
        val extractedFile = File(outDir, "log.txt")
        assertTrue(extractedFile.exists())
        assertEquals("Sample log content", extractedFile.readText())
    }

    @Test
    fun createAndExtractTarGz_compressed_success() = runBlocking {
        val srcDir = tempFolder.newFolder("src_targz")
        val file1 = File(srcDir, "info.json").apply { writeText("{\"version\": 7}") }

        val tarGzFile = File(tempFolder.root, "test.tar.gz")
        val createResult = handler.createArchive(
            sourceFiles = listOf(file1),
            destinationFile = tarGzFile,
            isGzip = true
        )
        assertTrue(createResult is ResultWrapper.Success)
        assertTrue(tarGzFile.exists())

        val listResult = handler.listEntries(tarGzFile, isGzip = true)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(1, entries.size)
        assertEquals("info.json", entries.first().name)

        val outDir = tempFolder.newFolder("out_targz")
        val extractResult = handler.extractArchive(
            archiveFile = tarGzFile,
            destinationDir = outDir,
            isGzip = true
        )
        assertTrue(extractResult is ResultWrapper.Success)
        val extractedFile = File(outDir, "info.json")
        assertTrue(extractedFile.exists())
        assertEquals("{\"version\": 7}", extractedFile.readText())
    }
}
