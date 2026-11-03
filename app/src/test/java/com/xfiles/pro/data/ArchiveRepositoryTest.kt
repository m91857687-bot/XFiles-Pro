package com.xfiles.pro.data

import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.repository.ArchiveRepositoryImpl
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.CompressionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArchiveRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: ArchiveRepositoryImpl

    private val testDispatchers = object : DispatchersProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setup() {
        repository = ArchiveRepositoryImpl(dispatchers = testDispatchers)
    }

    @Test
    fun listEntries_nonExistentFile_returnsError() = runBlocking {
        val nonExistent = File(tempFolder.root, "ghost.zip")
        val result = repository.listEntries(nonExistent)
        assertTrue(result is ResultWrapper.Error)
    }

    @Test
    fun createArchive_emptySourceFiles_returnsError() = runBlocking {
        val dest = File(tempFolder.root, "empty.zip")
        val result = repository.createArchive(
            sourceFiles = emptyList(),
            destinationArchiveFile = dest,
            format = ArchiveFormat.ZIP
        )
        assertTrue(result is ResultWrapper.Error)
    }

    @Test
    fun createArchive_rarFormat_returnsUnsupportedError() = runBlocking {
        val src = File(tempFolder.root, "test.txt").apply { writeText("Hello") }
        val dest = File(tempFolder.root, "test.rar")
        val result = repository.createArchive(
            sourceFiles = listOf(src),
            destinationArchiveFile = dest,
            format = ArchiveFormat.RAR
        )
        assertTrue(result is ResultWrapper.Error)
        assertTrue((result as ResultWrapper.Error).message?.contains("supported", ignoreCase = true) == true)
    }

    @Test
    fun createArchive_singleCompressorMultipleFiles_returnsError() = runBlocking {
        val f1 = File(tempFolder.root, "f1.txt").apply { writeText("1") }
        val f2 = File(tempFolder.root, "f2.txt").apply { writeText("2") }
        val dest = File(tempFolder.root, "out.gz")

        val result = repository.createArchive(
            sourceFiles = listOf(f1, f2),
            destinationArchiveFile = dest,
            format = ArchiveFormat.GZIP
        )
        assertTrue(result is ResultWrapper.Error)
    }

    @Test
    fun createAndExtractZip_viaRepository_success() = runBlocking {
        val src = File(tempFolder.root, "readme.md").apply { writeText("# XFiles Pro Archive Support") }
        val zipFile = File(tempFolder.root, "repo_test.zip")

        val createResult = repository.createArchive(
            sourceFiles = listOf(src),
            destinationArchiveFile = zipFile,
            format = ArchiveFormat.ZIP,
            compressionLevel = CompressionLevel.NORMAL
        )
        assertTrue(createResult is ResultWrapper.Success)

        val listResult = repository.listEntries(zipFile)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(1, entries.size)
        assertEquals("readme.md", entries.first().name)

        val outDir = tempFolder.newFolder("repo_out")
        val extractResult = repository.extractArchive(
            archiveFile = zipFile,
            destinationDir = outDir
        )
        assertTrue(extractResult is ResultWrapper.Success)
        val extracted = File(outDir, "readme.md")
        assertTrue(extracted.exists())
        assertEquals("# XFiles Pro Archive Support", extracted.readText())
    }

    @Test
    fun inPlaceZipModifications_viaRepository_success() = runBlocking {
        val f1 = File(tempFolder.root, "file1.txt").apply { writeText("Alpha") }
        val zipFile = File(tempFolder.root, "inplace.zip")

        repository.createArchive(listOf(f1), zipFile, ArchiveFormat.ZIP)

        val f2 = File(tempFolder.root, "file2.txt").apply { writeText("Beta") }
        val addResult = repository.addFilesToZip(zipFile, listOf(f2))
        assertTrue(addResult is ResultWrapper.Success)

        var listResult = repository.listEntries(zipFile)
        assertTrue(listResult is ResultWrapper.Success)
        assertEquals(2, (listResult as ResultWrapper.Success).data.size)

        val deleteResult = repository.deleteEntriesFromZip(zipFile, listOf("file1.txt"))
        assertTrue(deleteResult is ResultWrapper.Success)

        listResult = repository.listEntries(zipFile)
        assertTrue(listResult is ResultWrapper.Success)
        val entries = (listResult as ResultWrapper.Success).data
        assertEquals(1, entries.size)
        assertEquals("file2.txt", entries.first().name)
    }

    @Test
    fun isPasswordProtected_unencryptedZip_returnsFalse() = runBlocking {
        val f1 = File(tempFolder.root, "plain.txt").apply { writeText("text") }
        val zipFile = File(tempFolder.root, "plain.zip")
        repository.createArchive(listOf(f1), zipFile, ArchiveFormat.ZIP)

        val isProtected = repository.isPasswordProtected(zipFile)
        assertFalse(isProtected)
    }
}
