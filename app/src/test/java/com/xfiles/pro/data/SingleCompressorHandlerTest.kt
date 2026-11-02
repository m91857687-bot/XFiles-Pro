package com.xfiles.pro.data

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.archive.SingleCompressorHandler
import com.xfiles.pro.domain.model.ArchiveFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SingleCompressorHandlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var handler: SingleCompressorHandler

    @Before
    fun setup() {
        handler = SingleCompressorHandler()
    }

    @Test
    fun gzipCompressAndDecompress_success() = runBlocking {
        val srcFile = File(tempFolder.root, "notes.txt").apply {
            writeText("GZIP streaming compression test")
        }
        val gzFile = File(tempFolder.root, "notes.txt.gz")

        val compressResult = handler.compressFile(srcFile, gzFile, ArchiveFormat.GZIP)
        assertTrue(compressResult is ResultWrapper.Success)
        assertTrue(gzFile.exists())

        val outDir = tempFolder.newFolder("out_gz")
        val decompressResult = handler.decompressFile(gzFile, outDir, ArchiveFormat.GZIP)
        assertTrue(decompressResult is ResultWrapper.Success)

        val restored = File(outDir, "notes.txt")
        assertTrue(restored.exists())
        assertEquals("GZIP streaming compression test", restored.readText())
    }

    @Test
    fun bzip2CompressAndDecompress_success() = runBlocking {
        val srcFile = File(tempFolder.root, "data.csv").apply {
            writeText("id,name,value\n1,Alpha,100\n2,Beta,200\n")
        }
        val bz2File = File(tempFolder.root, "data.csv.bz2")

        val compressResult = handler.compressFile(srcFile, bz2File, ArchiveFormat.BZIP2)
        assertTrue(compressResult is ResultWrapper.Success)
        assertTrue(bz2File.exists())

        val outDir = tempFolder.newFolder("out_bz2")
        val decompressResult = handler.decompressFile(bz2File, outDir, ArchiveFormat.BZIP2)
        assertTrue(decompressResult is ResultWrapper.Success)

        val restored = File(outDir, "data.csv")
        assertTrue(restored.exists())
        assertEquals("id,name,value\n1,Alpha,100\n2,Beta,200\n", restored.readText())
    }

    @Test
    fun xzCompressAndDecompress_success() = runBlocking {
        val srcFile = File(tempFolder.root, "code.kt").apply {
            writeText("fun main() { println(\"XZ Works!\") }")
        }
        val xzFile = File(tempFolder.root, "code.kt.xz")

        val compressResult = handler.compressFile(srcFile, xzFile, ArchiveFormat.XZ)
        assertTrue(compressResult is ResultWrapper.Success)
        assertTrue(xzFile.exists())

        val outDir = tempFolder.newFolder("out_xz")
        val decompressResult = handler.decompressFile(xzFile, outDir, ArchiveFormat.XZ)
        assertTrue(decompressResult is ResultWrapper.Success)

        val restored = File(outDir, "code.kt")
        assertTrue(restored.exists())
        assertEquals("fun main() { println(\"XZ Works!\") }", restored.readText())
    }
}
