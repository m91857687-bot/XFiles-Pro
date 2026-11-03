package com.xfiles.pro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.database.XFilesDatabase
import com.xfiles.pro.data.local.download.DownloadEngine
import com.xfiles.pro.data.local.download.DownloadQueueManager
import com.xfiles.pro.data.repository.DownloadRepositoryImpl
import com.xfiles.pro.domain.model.DownloadRequest
import com.xfiles.pro.domain.model.DownloadStatus
import com.xfiles.pro.domain.model.StorageEvent
import com.xfiles.pro.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
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
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadRepositoryAndQueueTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: XFilesDatabase
    private lateinit var server: MockWebServer
    private lateinit var downloadDir: File

    private lateinit var queueManager: DownloadQueueManager
    private lateinit var repository: DownloadRepositoryImpl
    private val emittedStorageEvents = CopyOnWriteArrayList<StorageEvent>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, XFilesDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        server = MockWebServer()
        server.start()

        downloadDir = tempFolder.newFolder("downloads")

        val dispatchers = object : DispatchersProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }

        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val downloadEngine = DownloadEngine()

        var repoRef: DownloadRepositoryImpl? = null
        queueManager = DownloadQueueManager(
            downloadEngine = downloadEngine,
            downloadTaskDao = database.downloadTaskDao(),
            dispatchers = dispatchers,
            scope = testScope,
            maxConcurrentDownloads = 2,
            onDownloadCompletedListener = { task, file ->
                repoRef?.onDownloadFinished(task, file)
            }
        )

        // Mock StorageRepository to observe storage events
        val mockStorageRepo = object : StorageRepository {
            override suspend fun getStorageVolumes() = ResultWrapper.Success(emptyList<com.xfiles.pro.domain.model.StorageVolume>())
            override fun getPrimaryStorageVolume() = null
            override suspend fun listFiles(path: String, sortOption: com.xfiles.pro.domain.model.FileSortOption) = ResultWrapper.Success(emptyList<com.xfiles.pro.domain.model.FileItem>())
            override suspend fun createFolder(parentPath: String, folderName: String) = ResultWrapper.Success(com.xfiles.pro.domain.model.FileItem("", "", "", 0, 0, true, com.xfiles.pro.domain.model.FileType.FOLDER))
            override suspend fun createFile(parentPath: String, fileName: String, initialContent: ByteArray?) = ResultWrapper.Success(com.xfiles.pro.domain.model.FileItem("", "", "", 0, 0, false, com.xfiles.pro.domain.model.FileType.DOCUMENT))
            override suspend fun deleteFile(path: String) = ResultWrapper.Success(Unit)
            override suspend fun deleteFolder(path: String) = ResultWrapper.Success(Unit)
            override suspend fun renameFile(path: String, newName: String) = ResultWrapper.Success(com.xfiles.pro.domain.model.FileItem("", "", "", 0, 0, false, com.xfiles.pro.domain.model.FileType.DOCUMENT))
            override suspend fun copyFile(sourcePath: String, destinationDirectory: String, conflictResolution: com.xfiles.pro.domain.model.FileConflictResolution, onProgress: ((Float) -> Unit)?) = ResultWrapper.Success(com.xfiles.pro.domain.model.FileItem("", "", "", 0, 0, false, com.xfiles.pro.domain.model.FileType.DOCUMENT))
            override suspend fun moveFile(sourcePath: String, destinationDirectory: String, conflictResolution: com.xfiles.pro.domain.model.FileConflictResolution) = ResultWrapper.Success(com.xfiles.pro.domain.model.FileItem("", "", "", 0, 0, false, com.xfiles.pro.domain.model.FileType.DOCUMENT))
            override val storageEvents = kotlinx.coroutines.flow.MutableSharedFlow<StorageEvent>()
            override fun notifyStorageEvent(event: StorageEvent) {
                emittedStorageEvents.add(event)
            }
            override fun getPersistedSafPermissions() = emptyList<com.xfiles.pro.domain.model.SafPermission>()
            override fun releaseSafPermission(uri: android.net.Uri) {}
            override fun hasSafPermissionForUri(uri: android.net.Uri) = true
        }

        repository = DownloadRepositoryImpl(
            context = context,
            dispatchers = dispatchers,
            downloadTaskDao = database.downloadTaskDao(),
            queueManager = queueManager,
            storageRepository = mockStorageRepo
        ).also { repoRef = it }
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    @Test
    fun `enqueuing download inserts task into Room and triggers download`() = runBlocking {
        val payload = "Testing Repository and Queue Integration".toByteArray()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", payload.size)
                .setBody(Buffer().write(payload))
        )

        val url = server.url("/doc.txt").toString()
        val result = repository.enqueueDownload(
            DownloadRequest(url = url, destinationDirectory = downloadDir.absolutePath)
        )

        assertTrue(result is ResultWrapper.Success)
        val taskId = (result as ResultWrapper.Success).data

        val taskInDb = database.downloadTaskDao().getTaskById(taskId)
        assertNotNull(taskInDb)
        assertEquals("doc.txt", taskInDb!!.fileName)

        // Verify task eventually reaches COMPLETED state in database
        val allDownloads = repository.getAllDownloads().first()
        val item = allDownloads.firstOrNull { it.id == taskId }
        assertNotNull(item)
        assertEquals(DownloadStatus.COMPLETED, item!!.status)
        assertEquals(payload.size.toLong(), item.downloadedBytes)

        // Verify storage event was emitted for File Manager & Indexing integration
        assertTrue(emittedStorageEvents.any { it is StorageEvent.Created && it.path.endsWith("doc.txt") })
    }

    @Test
    fun `respects concurrency limit by queueing tasks beyond maximum limit`() = runBlocking {
        // Prepare 3 tasks when max is 2
        for (i in 1..3) {
            val payload = "Data for task $i".toByteArray()
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Length", payload.size)
                    .setBody(Buffer().write(payload))
            )
        }

        val res1 = repository.enqueueDownload(DownloadRequest(server.url("/1.bin").toString(), destinationDirectory = downloadDir.absolutePath))
        val res2 = repository.enqueueDownload(DownloadRequest(server.url("/2.bin").toString(), destinationDirectory = downloadDir.absolutePath))
        val res3 = repository.enqueueDownload(DownloadRequest(server.url("/3.bin").toString(), destinationDirectory = downloadDir.absolutePath))

        assertTrue(res1 is ResultWrapper.Success)
        assertTrue(res2 is ResultWrapper.Success)
        assertTrue(res3 is ResultWrapper.Success)

        // Verify all 3 tasks were persisted and ultimately processed without conflict
        val allTasks = repository.getAllDownloads().first()
        assertEquals(3, allTasks.size)
        assertTrue(allTasks.all { it.status == DownloadStatus.COMPLETED })
    }

    @Test
    fun `cancel download removes partial part file and marks as cancelled`() = runBlocking {
        val payload = ByteArray(100 * 1024) { 0x01 }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", payload.size)
                .setBody(Buffer().write(payload))
        )

        val res = repository.enqueueDownload(DownloadRequest(server.url("/cancel_test.bin").toString(), destinationDirectory = downloadDir.absolutePath))
        val taskId = (res as ResultWrapper.Success).data

        repository.cancelDownload(taskId)

        val task = database.downloadTaskDao().getTaskById(taskId)
        assertNotNull(task)
        assertEquals(DownloadStatus.CANCELLED.name, task!!.status)

        val partFile = File(downloadDir, "cancel_test.bin.part")
        assertFalse(partFile.exists())
    }

    @Test
    fun `rename completed download updates file on disk and database record`() = runBlocking {
        val file = File(downloadDir, "old_name.txt")
        file.writeText("Sample content to rename")

        val entity = com.xfiles.pro.data.local.database.entity.DownloadTaskEntity(
            id = "rename_id",
            url = "https://example.com/old_name.txt",
            fileName = "old_name.txt",
            destinationDirectory = downloadDir.absolutePath,
            destinationFilePath = file.absolutePath,
            totalBytes = 24L,
            downloadedBytes = 24L,
            status = DownloadStatus.COMPLETED.name,
            mimeType = "text/plain",
            eTag = null,
            lastModified = null,
            isResumable = true,
            retryCount = 0,
            errorMessage = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.downloadTaskDao().insertTask(entity)

        val renameResult = repository.renameCompletedDownload("rename_id", "new_name.txt")
        assertTrue(renameResult is ResultWrapper.Success)

        val updatedTask = database.downloadTaskDao().getTaskById("rename_id")
        assertNotNull(updatedTask)
        assertEquals("new_name.txt", updatedTask!!.fileName)
        assertTrue(File(downloadDir, "new_name.txt").exists())
        assertFalse(File(downloadDir, "old_name.txt").exists())

        // Verify StorageEvent.Moved emitted
        assertTrue(emittedStorageEvents.any { it is StorageEvent.Moved })
    }

    @Test
    fun `delete download removes record and optionally deletes file on disk`() = runBlocking {
        val file = File(downloadDir, "delete_target.txt")
        file.writeText("File to delete")

        val entity = com.xfiles.pro.data.local.database.entity.DownloadTaskEntity(
            id = "delete_id",
            url = "https://example.com/delete.txt",
            fileName = "delete_target.txt",
            destinationDirectory = downloadDir.absolutePath,
            destinationFilePath = file.absolutePath,
            totalBytes = 14L,
            downloadedBytes = 14L,
            status = DownloadStatus.COMPLETED.name,
            mimeType = "text/plain",
            eTag = null,
            lastModified = null,
            isResumable = true,
            retryCount = 0,
            errorMessage = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        database.downloadTaskDao().insertTask(entity)

        val deleteResult = repository.deleteDownload("delete_id", deleteFile = true)
        assertTrue(deleteResult is ResultWrapper.Success)

        // Record removed from Room
        assertNull(database.downloadTaskDao().getTaskById("delete_id"))
        // File removed from disk
        assertFalse(file.exists())
        // StorageEvent.Deleted emitted
        assertTrue(emittedStorageEvents.any { it is StorageEvent.Deleted })
    }

    @Test
    fun `clear completed downloads cleans up all completed records from database`() = runBlocking {
        val entity1 = com.xfiles.pro.data.local.database.entity.DownloadTaskEntity(
            id = "c1",
            url = "https://example.com/c1.txt",
            fileName = "c1.txt",
            destinationDirectory = downloadDir.absolutePath,
            destinationFilePath = File(downloadDir, "c1.txt").absolutePath,
            totalBytes = 10L,
            downloadedBytes = 10L,
            status = DownloadStatus.COMPLETED.name,
            mimeType = null,
            eTag = null,
            lastModified = null,
            isResumable = true,
            retryCount = 0,
            errorMessage = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val entity2 = entity1.copy(id = "p1", status = DownloadStatus.PAUSED.name)
        database.downloadTaskDao().insertTask(entity1)
        database.downloadTaskDao().insertTask(entity2)

        repository.clearCompletedDownloads()

        assertNull(database.downloadTaskDao().getTaskById("c1"))
        assertNotNull(database.downloadTaskDao().getTaskById("p1"))
    }
}
