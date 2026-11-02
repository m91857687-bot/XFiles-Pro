package com.xfiles.pro.data.local.archive

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.ArchiveSecurityPolicy
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Handler for TAR, TAR.GZ and TGZ archives using Apache Commons Compress.
 */
class TarArchiveHandler {

    private fun openTarInputStream(archiveFile: File, isGzip: Boolean): TarArchiveInputStream {
        val fileStream = FileInputStream(archiveFile)
        val bufferedStream = BufferedInputStream(fileStream, ArchiveBufferConstants.BUFFER_SIZE)
        val inStream: InputStream = if (isGzip) {
            GzipCompressorInputStream(bufferedStream)
        } else {
            bufferedStream
        }
        return TarArchiveInputStream(inStream)
    }

    private fun openTarOutputStream(destinationFile: File, isGzip: Boolean): TarArchiveOutputStream {
        val fileStream = FileOutputStream(destinationFile)
        val bufferedStream = BufferedOutputStream(fileStream, ArchiveBufferConstants.BUFFER_SIZE)
        val outStream: OutputStream = if (isGzip) {
            GzipCompressorOutputStream(bufferedStream)
        } else {
            bufferedStream
        }
        return TarArchiveOutputStream(outStream).apply {
            setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
        }
    }

    suspend fun listEntries(archiveFile: File, isGzip: Boolean): ResultWrapper<List<ArchiveEntryItem>> {
        return try {
            val items = mutableListOf<ArchiveEntryItem>()
            openTarInputStream(archiveFile, isGzip).use { tarIn ->
                var entry: TarArchiveEntry? = tarIn.nextEntry
                while (entry != null) {
                    val cleanPath = entry.name.replace('\\', '/')
                    val name = cleanPath.trimEnd('/').substringAfterLast('/')
                    items.add(
                        ArchiveEntryItem(
                            path = cleanPath,
                            name = if (name.isEmpty()) cleanPath else name,
                            isDirectory = entry.isDirectory,
                            uncompressedSize = entry.size,
                            compressedSize = entry.size,
                            lastModified = entry.modTime.time,
                            isEncrypted = false
                        )
                    )
                    entry = tarIn.nextEntry
                }
            }
            ResultWrapper.Success(items)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to list TAR entries: ${e.message}")
        }
    }

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationFile: File,
        isGzip: Boolean,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<File> {
        return try {
            destinationFile.parentFile?.mkdirs()
            val totalCount = countTotalFiles(sourceFiles)
            var completedCount = 0

            openTarOutputStream(destinationFile, isGzip).use { tarOut ->
                for (file in sourceFiles) {
                    addFileToTar(tarOut, file, "", totalCount, { completedCount++ }) { currentItem ->
                        onProgress(
                            ArchiveProgress(
                                currentItem = currentItem,
                                completedItems = completedCount,
                                totalItems = totalCount,
                                percentage = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                            )
                        )
                    }
                }
            }

            onProgress(
                ArchiveProgress(
                    currentItem = destinationFile.name,
                    completedItems = totalCount,
                    totalItems = totalCount,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(destinationFile)
        } catch (e: CancellationException) {
            destinationFile.delete()
            throw e
        } catch (e: Exception) {
            destinationFile.delete()
            ResultWrapper.Error(e, "Failed to create TAR archive: ${e.message}")
        }
    }

    private suspend fun addFileToTar(
        tarOut: TarArchiveOutputStream,
        file: File,
        basePath: String,
        total: Int,
        onFileAdded: () -> Unit,
        onProgressItem: (String) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
        onProgressItem(file.name)

        if (file.isDirectory) {
            val dirEntry = TarArchiveEntry(file, "$entryName/")
            tarOut.putArchiveEntry(dirEntry)
            tarOut.closeArchiveEntry()
            onFileAdded()

            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                addFileToTar(tarOut, child, entryName, total, onFileAdded, onProgressItem)
            }
        } else {
            val fileEntry = TarArchiveEntry(file, entryName).apply {
                size = file.length()
                modTime = java.util.Date(file.lastModified())
            }
            tarOut.putArchiveEntry(fileEntry)
            FileInputStream(file).use { input ->
                val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    tarOut.write(buffer, 0, len)
                }
            }
            tarOut.closeArchiveEntry()
            onFileAdded()
        }
    }

    suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
        isGzip: Boolean,
        selectedEntries: List<String>? = null,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Int> {
        return try {
            destinationDir.mkdirs()
            val selectedSet = selectedEntries?.map { it.replace('\\', '/') }?.toSet()

            var extractedCount = 0
            val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)
            var totalUncompressedWritten = 0L

            openTarInputStream(archiveFile, isGzip).use { tarIn ->
                var entry: TarArchiveEntry? = tarIn.nextEntry
                while (entry != null) {
                    currentCoroutineContext().ensureActive()

                    val cleanPath = entry.name.replace('\\', '/')
                    val shouldExtract = selectedSet == null ||
                            selectedSet.contains(cleanPath) ||
                            selectedSet.any { cleanPath.startsWith("$it/") }

                    if (!shouldExtract) {
                        entry = tarIn.nextEntry
                        continue
                    }

                    val targetFile = ArchiveSecurityPolicy.resolveAndValidateDestination(destinationDir, cleanPath)
                    onProgress(
                        ArchiveProgress(
                            currentItem = cleanPath.substringAfterLast('/'),
                            completedItems = extractedCount,
                            totalItems = extractedCount + 1,
                            percentage = 0.5f,
                            isIndeterminate = true
                        )
                    )

                    if (entry.isDirectory || cleanPath.endsWith('/')) {
                        targetFile.mkdirs()
                        extractedCount++
                        entry = tarIn.nextEntry
                        continue
                    }

                    targetFile.parentFile?.mkdirs()

                    val finalFile = if (targetFile.exists()) {
                        when (conflictResolution) {
                            FileConflictResolution.SKIP -> {
                                extractedCount++
                                entry = tarIn.nextEntry
                                continue
                            }
                            FileConflictResolution.OVERWRITE -> {
                                targetFile.delete()
                                targetFile
                            }
                            FileConflictResolution.AUTO_RENAME -> resolveAutoRename(targetFile)
                        }
                    } else {
                        targetFile
                    }

                    var entryWritten = 0L
                    FileOutputStream(finalFile).use { out ->
                        var len: Int
                        while (tarIn.read(buffer).also { len = it } != -1) {
                            out.write(buffer, 0, len)
                            entryWritten += len
                            totalUncompressedWritten += len
                            ArchiveSecurityPolicy.checkBombThresholds(
                                compressedBytesRead = (archiveFile.length() / 2).coerceAtLeast(1L),
                                uncompressedBytesWritten = totalUncompressedWritten,
                                singleEntryBytesWritten = entryWritten
                            )
                        }
                    }

                    if (entry.modTime != null && entry.modTime.time > 0) {
                        finalFile.setLastModified(entry.modTime.time)
                    }

                    extractedCount++
                    entry = tarIn.nextEntry
                }
            }

            onProgress(
                ArchiveProgress(
                    currentItem = "Completed",
                    completedItems = extractedCount,
                    totalItems = extractedCount,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(extractedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to extract TAR: ${e.message}")
        }
    }

    private fun countTotalFiles(files: List<File>): Int {
        var count = 0
        for (f in files) {
            count++
            if (f.isDirectory) {
                count += countTotalFiles(f.listFiles()?.toList() ?: emptyList())
            }
        }
        return count
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
