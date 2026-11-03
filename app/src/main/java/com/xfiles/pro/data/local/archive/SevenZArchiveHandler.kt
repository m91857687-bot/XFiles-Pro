package com.xfiles.pro.data.local.archive

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.ArchiveSecurityPolicy
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZMethod
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Handler for 7-Zip (.7z) archives using Apache Commons Compress SevenZFile and SevenZOutputFile.
 */
class SevenZArchiveHandler {

    suspend fun listEntries(archiveFile: File): ResultWrapper<List<ArchiveEntryItem>> {
        return try {
            val items = mutableListOf<ArchiveEntryItem>()
            SevenZFile.builder().setFile(archiveFile).get().use { sevenZ ->
                for (entry in sevenZ.entries) {
                    val cleanPath = entry.name.replace('\\', '/')
                    val name = cleanPath.trimEnd('/').substringAfterLast('/')
                    items.add(
                        ArchiveEntryItem(
                            path = cleanPath,
                            name = if (name.isEmpty()) cleanPath else name,
                            isDirectory = entry.isDirectory,
                            uncompressedSize = entry.size,
                            compressedSize = entry.size,
                            lastModified = entry.lastModifiedDate?.time ?: 0L,
                            isEncrypted = entry.hasStream() && entry.size > 0 && false
                        )
                    )
                }
            }
            ResultWrapper.Success(items)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to list 7z entries: ${e.message}")
        }
    }

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationFile: File,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<File> {
        return try {
            destinationFile.parentFile?.mkdirs()
            val totalCount = countTotalFiles(sourceFiles)
            var completedCount = 0

            SevenZOutputFile(destinationFile).use { out ->
                val method = when (compressionLevel) {
                    CompressionLevel.STORE -> SevenZMethod.COPY
                    else -> SevenZMethod.LZMA2
                }
                out.setContentCompression(method)

                for (file in sourceFiles) {
                    addFileToSevenZ(out, file, "", totalCount, { completedCount++ }) { currentItem ->
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
            ResultWrapper.Error(e, "Failed to create 7z archive: ${e.message}")
        }
    }

    private suspend fun addFileToSevenZ(
        sevenZOut: SevenZOutputFile,
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
            val dirEntry = sevenZOut.createArchiveEntry(file, "$entryName/")
            sevenZOut.putArchiveEntry(dirEntry)
            sevenZOut.closeArchiveEntry()
            onFileAdded()

            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                addFileToSevenZ(sevenZOut, child, entryName, total, onFileAdded, onProgressItem)
            }
        } else {
            val fileEntry = sevenZOut.createArchiveEntry(file, entryName)
            sevenZOut.putArchiveEntry(fileEntry)
            FileInputStream(file).use { input ->
                val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    sevenZOut.write(buffer, 0, len)
                }
            }
            sevenZOut.closeArchiveEntry()
            onFileAdded()
        }
    }

    suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
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

            SevenZFile.builder().setFile(archiveFile).get().use { sevenZ ->
                var entry: SevenZArchiveEntry? = sevenZ.nextEntry
                while (entry != null) {
                    currentCoroutineContext().ensureActive()

                    val cleanPath = entry.name.replace('\\', '/')
                    val shouldExtract = selectedSet == null ||
                            selectedSet.contains(cleanPath) ||
                            selectedSet.any { cleanPath.startsWith("$it/") }

                    if (!shouldExtract) {
                        entry = sevenZ.nextEntry
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
                        entry = sevenZ.nextEntry
                        continue
                    }

                    targetFile.parentFile?.mkdirs()

                    val finalFile = if (targetFile.exists()) {
                        when (conflictResolution) {
                            FileConflictResolution.SKIP -> {
                                extractedCount++
                                entry = sevenZ.nextEntry
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
                        while (sevenZ.read(buffer).also { len = it } != -1) {
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

                    if (entry.lastModifiedDate != null) {
                        finalFile.setLastModified(entry.lastModifiedDate.time)
                    }

                    extractedCount++
                    entry = sevenZ.nextEntry
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
            ResultWrapper.Error(e, "Failed to extract 7z: ${e.message}")
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
