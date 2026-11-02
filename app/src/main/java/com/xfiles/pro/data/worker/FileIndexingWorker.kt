package com.xfiles.pro.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.xfiles.pro.XFilesApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for performing persistent background file indexing
 * with battery awareness, cancellation checks, and progress reporting.
 */
class FileIndexingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as? XFilesApplication
            ?: return@withContext Result.failure()

        val repository = app.container.fileIndexingRepository

        try {
            setProgress(workDataOf(KEY_STATUS to STATUS_RUNNING, KEY_PROGRESS to 0f))

            if (isStopped) {
                return@withContext Result.retry()
            }

            val result = repository.runIndexing(forceRescan = false)

            if (result.isSuccess) {
                val indexed = result.getOrDefault(0)
                setProgress(workDataOf(KEY_STATUS to STATUS_COMPLETED, KEY_INDEXED_COUNT to indexed))
                Result.success(workDataOf(KEY_INDEXED_COUNT to indexed))
            } else {
                Result.retry()
            }
        } catch (t: Throwable) {
            if (isStopped) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR to (t.message ?: "Unknown indexing error")))
            }
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "XFilesIndexingWorker_Periodic"
        const val UNIQUE_ONE_TIME_WORK_NAME = "XFilesIndexingWorker_OneTime"

        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"
        const val KEY_INDEXED_COUNT = "indexed_count"
        const val KEY_ERROR = "error"

        const val STATUS_RUNNING = "running"
        const val STATUS_COMPLETED = "completed"
    }
}
