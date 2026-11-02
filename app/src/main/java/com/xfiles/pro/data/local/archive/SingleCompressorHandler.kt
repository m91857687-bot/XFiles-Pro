package com.xfiles.pro.data.local.archive

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.ArchiveSecurityPolicy
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Handler for single-stream compression formats: GZIP (.gz), BZIP2 (.bz2), and XZ (.xz).
 */
class SingleCompressorHandler {

    private fun createCompressorInputStream(file: File, format: ArchiveFormat): InputStream {
        val bis = BufferedInputStream(FileInputStream(file), ArchiveBufferConstants.BUFFER_SIZE)
        return when (format) {
            ArchiveFormat.GZIP -> GzipCompressorInputStream(bis)
            ArchiveFormat.BZIP2 -> BZip2CompressorInputStream(bis)
            ArchiveFormat.XZ -> XZCompressorInputStream(bis)
            else -> throw IllegalArgumentException("Unsupported single compressor format: $format")
        }
    }

    private fun createCompressorOutputStream(file: File, format: ArchiveFormat): OutputStream {
        val bos = BufferedOutputStream(FileOutputStream(file), ArchiveBufferConstants.BUFFER_SIZE)
        return when (format) {
            ArchiveFormat.GZIP -> GzipCompressorOutputStream(bos)
            ArchiveFormat.BZIP2 -> BZip2CompressorOutputStream(bos)
            ArchiveFormat.XZ -> XZCompressorOutputStream(bos)
            else -> throw IllegalArgumentException("Unsupported single compressor format: $format")
        }
    }

    fun listEntries(archiveFile: File, format: ArchiveFormat): ResultWrapper<List<ArchiveEntryItem>> {
        val innerName = archiveFile.nameWithoutExtension
        val entry = ArchiveEntryItem(
            path = innerName,
            name = innerName,
            isDirectory = false,
            uncompressedSize = 0L,
            compressedSize = archiveFile.length(),
            lastModified = archiveFile.lastModified(),
            isEncrypted = false
        )
        return ResultWrapper.Success(listOf(entry))
    }

    suspend fun compressFile(
        sourceFile: File,
        destinationFile: File,
        format: ArchiveFormat,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<File> {
        return try {
            destinationFile.parentFile?.mkdirs()
            val totalBytes = sourceFile.length()
            var processed = 0L

            onProgress(
                ArchiveProgress(
                    currentItem = sourceFile.name,
                    completedItems = 0,
                    totalItems = 1,
                    processedBytes = 0L,
                    totalBytes = totalBytes,
                    percentage = 0f
                )
            )

            createCompressorOutputStream(destinationFile, format).use { out ->
                FileInputStream(sourceFile).use { input ->
                    val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        out.write(buffer, 0, len)
                        processed += len
                        onProgress(
                            ArchiveProgress(
                                currentItem = sourceFile.name,
                                completedItems = 0,
                                totalItems = 1,
                                processedBytes = processed,
                                totalBytes = totalBytes,
                                percentage = if (totalBytes > 0) processed.toFloat() / totalBytes else 0.5f
                            )
                        )
                    }
                }
            }

            onProgress(
                ArchiveProgress(
                    currentItem = destinationFile.name,
                    completedItems = 1,
                    totalItems = 1,
                    processedBytes = totalBytes,
                    totalBytes = totalBytes,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(destinationFile)
        } catch (e: CancellationException) {
            destinationFile.delete()
            throw e
        } catch (e: Exception) {
            destinationFile.delete()
            ResultWrapper.Error(e, "Compression failed: ${e.message}")
        }
    }

    suspend fun decompressFile(
        archiveFile: File,
        destinationDir: File,
        format: ArchiveFormat,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Int> {
        return try {
            destinationDir.mkdirs()
            val innerName = archiveFile.nameWithoutExtension
            val targetFile = ArchiveSecurityPolicy.resolveAndValidateDestination(destinationDir, innerName)

            val finalFile = if (targetFile.exists()) {
                when (conflictResolution) {
                    FileConflictResolution.SKIP -> return ResultWrapper.Success(0)
                    FileConflictResolution.OVERWRITE -> {
                        targetFile.delete()
                        targetFile
                    }
                    FileConflictResolution.AUTO_RENAME -> resolveAutoRename(targetFile)
                }
            } else {
                targetFile
            }

            onProgress(
                ArchiveProgress(
                    currentItem = innerName,
                    completedItems = 0,
                    totalItems = 1,
                    percentage = 0.1f
                )
            )

            var written = 0L
            createCompressorInputStream(archiveFile, format).use { input ->
                FileOutputStream(finalFile).use { out ->
                    val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        out.write(buffer, 0, len)
                        written += len
                        ArchiveSecurityPolicy.checkBombThresholds(
                            compressedBytesRead = archiveFile.length().coerceAtLeast(1L),
                            uncompressedBytesWritten = written,
                            singleEntryBytesWritten = written
                        )
                    }
                }
            }

            onProgress(
                ArchiveProgress(
                    currentItem = finalFile.name,
                    completedItems = 1,
                    totalItems = 1,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Decompression failed: ${e.message}")
        }
    }

    private fun resolveAutoRename(file: File): File {
        val parent = file.parentFile ?: return file
        val name = file.nameWithoutExtension
        val ext = file.extension
        val dotExt = if (ext.isNotEmpty()) ".$ext" else ""

        var counter = 1
        var candidate = File(parent, "$name ($counter)$dotExt")
        while (candidate.exists()) {
            counter++
            candidate = File(parent, "$name ($counter)$dotExt")
        }
        return candidate
    }
}
