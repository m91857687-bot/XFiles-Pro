package com.xfiles.pro.data

import com.xfiles.pro.data.local.download.DownloadEngine
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class DownloadEngineRealHttpTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var engine: DownloadEngine
    private lateinit var downloadDir: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        engine = DownloadEngine(client = DownloadEngine.defaultClient())
        downloadDir = tempFolder.newFolder("downloads")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `downloads full file successfully with streaming and correct content`() = runBlocking {
        val payload = "Hello World from XFiles Pro Download Engine!".toByteArray()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setHeader("Content-Length", payload.size)
                .setHeader("Accept-Ranges", "bytes")
                .setHeader("ETag", "\"v1-hash\"")
                .setBody(Buffer().write(payload))
        )

        val url = server.url("/test.txt").toString()
        var lastDownloaded = 0L
        var reportedTotal = 0L

        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "test.txt",
            listener = { downloaded, total, _, _ ->
                lastDownloaded = downloaded
                reportedTotal = total
            }
        )

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertTrue(result.finalFile!!.exists())
        assertEquals(payload.size.toLong(), result.finalFile!!.length())
        assertEquals(payload.size.toLong(), result.downloadedBytes)
        assertEquals(payload.size.toLong(), result.totalBytes)
        assertEquals("\"v1-hash\"", result.eTag)
        assertTrue(result.isResumable)
        assertArrayEquals(payload, result.finalFile!!.readBytes())
    }

    @Test
    fun `downloads large file via streaming without loading full content into RAM`() = runBlocking {
        // Create 2 MB payload of repeating bytes
        val chunkSize = 64 * 1024
        val totalChunks = 32 // 2 MB
        val buffer = Buffer()
        val chunk = ByteArray(chunkSize) { 0x42 }
        for (i in 0 until totalChunks) {
            buffer.write(chunk)
        }
        val expectedSize = (chunkSize * totalChunks).toLong()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", expectedSize)
                .setBody(buffer)
        )

        val url = server.url("/large.bin").toString()
        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "large.bin"
        )

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertEquals(expectedSize, result.finalFile!!.length())
    }

    @Test
    fun `downloads file with unknown Content-Length (chunked transfer)`() = runBlocking {
        val payload = "Chunked transfer test stream data".toByteArray()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Transfer-Encoding", "chunked")
                .setBody(Buffer().write(payload))
        )

        val url = server.url("/chunked.dat").toString()
        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "chunked.dat"
        )

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertEquals(payload.size.toLong(), result.finalFile!!.length())
        assertEquals(payload.size.toLong(), result.downloadedBytes)
    }

    @Test
    fun `resumes download with HTTP Range when partial file exists`() = runBlocking {
        val fullData = "0123456789ABCDEF0123456789ABCDEF".toByteArray() // 32 bytes
        val partialLength = 16

        // Prepare existing .part file with first 16 bytes
        val partFile = File(downloadDir, "resumable.bin.part")
        partFile.writeBytes(fullData.copyOfRange(0, partialLength))

        // Remaining 16 bytes for 206 Partial Content
        val remainingData = fullData.copyOfRange(partialLength, fullData.size)
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes $partialLength-${fullData.size - 1}/${fullData.size}")
                .setHeader("Content-Length", remainingData.size)
                .setBody(Buffer().write(remainingData))
        )

        val url = server.url("/resumable.bin").toString()
        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "resumable.bin",
            initialETag = "\"etag-123\""
        )

        // Verify sent Range header
        val recordedRequest = server.takeRequest()
        assertEquals("bytes=16-", recordedRequest.getHeader("Range"))
        assertEquals("\"etag-123\"", recordedRequest.getHeader("If-Range"))

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertEquals(fullData.size.toLong(), result.finalFile!!.length())
        assertArrayEquals(fullData, result.finalFile!!.readBytes())
        assertFalse(partFile.exists()) // .part cleaned up / renamed
    }

    @Test
    fun `restarts from beginning when server returns 200 OK instead of 206 on resume request`() = runBlocking {
        // Prepare corrupted or old .part file
        val partFile = File(downloadDir, "fallback.bin.part")
        partFile.writeBytes("OLD PARTIAL GARBAGE DATA".toByteArray())

        val freshData = "FRESH FULL DATA FROM SERVER".toByteArray()
        server.enqueue(
            MockResponse()
                .setResponseCode(200) // Server ignored Range header and sends entire file
                .setHeader("Content-Length", freshData.size)
                .setBody(Buffer().write(freshData))
        )

        val url = server.url("/fallback.bin").toString()
        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "fallback.bin"
        )

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertEquals(freshData.size.toLong(), result.finalFile!!.length())
        // Verified: The old garbage partial data was NOT appended to the new file!
        assertArrayEquals(freshData, result.finalFile!!.readBytes())
    }

    @Test
    fun `handles HTTP 416 Range Not Satisfiable by restarting from byte 0`() = runBlocking {
        val partFile = File(downloadDir, "range416.bin.part")
        partFile.writeBytes(ByteArray(100))

        val fullPayload = "Entire File Content Restarted".toByteArray()

        // 1st response: 416
        server.enqueue(MockResponse().setResponseCode(416))
        // 2nd response: 200 with full body
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", fullPayload.size)
                .setBody(Buffer().write(fullPayload))
        )

        val url = server.url("/range416.bin").toString()
        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "range416.bin"
        )

        assertNull(result.error)
        assertNotNull(result.finalFile)
        assertEquals(fullPayload.size.toLong(), result.finalFile!!.length())
        assertArrayEquals(fullPayload, result.finalFile!!.readBytes())
    }

    @Test
    fun `handles HTTP 404 and 500 error responses gracefully`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not Found"))

        val url404 = server.url("/missing.zip").toString()
        val result404 = engine.download(
            url = url404,
            destinationDirectory = downloadDir,
            fileName = "missing.zip"
        )
        assertNotNull(result404.error)
        assertTrue(result404.error?.message?.contains("404") == true)
        assertNull(result404.finalFile)

        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))
        val url500 = server.url("/error.zip").toString()
        val result500 = engine.download(
            url = url500,
            destinationDirectory = downloadDir,
            fileName = "error.zip"
        )
        assertNotNull(result500.error)
        assertTrue(result500.error?.message?.contains("500") == true)
        assertNull(result500.finalFile)
    }

    @Test
    fun `cleans up part file when download is cancelled by user`() = runBlocking {
        val payload = ByteArray(256 * 1024) { 0x5A }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", payload.size)
                .setBody(Buffer().write(payload))
        )

        val isCancelled = AtomicBoolean(false)
        val url = server.url("/cancel.bin").toString()

        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "cancel.bin",
            isCancelled = isCancelled,
            listener = { downloaded, _, _, _ ->
                if (downloaded > 1024) {
                    isCancelled.set(true)
                }
            }
        )

        assertNotNull(result.error)
        val partFile = File(downloadDir, "cancel.bin.part")
        val finalFile = File(downloadDir, "cancel.bin")
        assertFalse("Part file must be deleted on cancellation", partFile.exists())
        assertFalse("Final file must not exist on cancellation", finalFile.exists())
    }

    @Test
    fun `retains part file when download is paused`() = runBlocking {
        val payload = ByteArray(256 * 1024) { 0x7E }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", payload.size)
                .setBody(Buffer().write(payload))
        )

        val isPaused = AtomicBoolean(false)
        val url = server.url("/pause.bin").toString()

        val result = engine.download(
            url = url,
            destinationDirectory = downloadDir,
            fileName = "pause.bin",
            isPaused = isPaused,
            listener = { downloaded, _, _, _ ->
                if (downloaded > 1024) {
                    isPaused.set(true)
                }
            }
        )

        assertNull(result.error)
        assertNull(result.finalFile)
        val partFile = File(downloadDir, "pause.bin.part")
        assertTrue("Part file must be retained on pause for resumption", partFile.exists())
        assertTrue(partFile.length() > 0)
    }

    @Test
    fun `rejects unsupported URL schemes`() = runBlocking {
        val result = engine.download(
            url = "ftp://ftp.example.com/file.iso",
            destinationDirectory = downloadDir,
            fileName = "file.iso"
        )
        assertNotNull(result.error)
        assertTrue(result.error is IllegalArgumentException)
    }
}
