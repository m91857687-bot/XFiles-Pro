package com.xfiles.pro.data.local.archive

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.ArchiveSecurityPolicy
import com.xfiles.pro.domain.model.FileConflictResolution
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream

/**
 * Handler for RAR archives (read and extraction) using Junrar.
 * Note: RAR creation is not supported as the compression format is proprietary.
 */
class RarArchiveHandler {

    fun isEncrypted(archiveFile: File): Boolean {
        return try {
            Archive(archiveFile).use { rar ->
                rar.isEncrypted || rar.fileHeaders.any { it.isEncrypted }
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listEntries(
        archiveFile: File,
        password: String? = null
    ): ResultWrapper<List<ArchiveEntryItem>> {
        return try {
            val items = mutableListOf<ArchiveEntryItem>()
            Archive(archiveFile, password).use { rar ->
                if (rar.isEncrypted && password.isNullOrEmpty()) {
                    return ResultWrapper.Error(IllegalStateException("RAR archive is password protected"), "RAR archive is password protected")
                }

                for (header in rar.fileHeaders) {
                    val rawName = header.fileName ?: ""
                    val cleanPath = rawName.replace('\\', '/')
                    val name = cleanPath.trimEnd('/').substringAfterLast('/')
                    items.add(
                        ArchiveEntryItem(
                            path = cleanPath,
                            name = if (name.isEmpty()) cleanPath else name,
                            isDirectory = header.isDirectory,
                            uncompressedSize = header.unpSize,
                            compressedSize = header.packSize,
                            lastModified = header.mTime?.time ?: 0L,
                            isEncrypted = header.isEncrypted
                        )
                    )
                }
            }
            ResultWrapper.Success(items)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to list RAR entries: ${e.message}")
        }
    }

    suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
        password: String? = null,
        selectedEntries: List<String>? = null,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Int> {
        return try {
            destinationDir.mkdirs()
            val selectedSet = selectedEntries?.map { it.replace('\\', '/') }?.toSet()

            var extractedCount = 0
            var totalUncompressed = 0L

            Archive(archiveFile, password).use { rar ->
                if (rar.isEncrypted && password.isNullOrEmpty()) {
                    return ResultWrapper.Error(IllegalStateException("RAR archive is password protected"), "RAR archive is password protected")
                }

                val targetHeaders = if (selectedSet != null) {
                    rar.fileHeaders.filter { header ->
                        val cleanPath = (header.fileName ?: "").replace('\\', '/')
                        selectedSet.contains(cleanPath) || selectedSet.any { cleanPath.startsWith("$it/") }
                    }
                } else {
                    rar.fileHeaders
                }

                val totalCount = targetHeaders.size

                for (header in targetHeaders) {
                    currentCoroutineContext().ensureActive()

                    val rawName = header.fileName ?: ""
                    val cleanPath = rawName.replace('\\', '/')
                    val targetFile = ArchiveSecurityPolicy.resolveAndValidateDestination(destinationDir, cleanPath)

                    onProgress(
                        ArchiveProgress(
                            currentItem = cleanPath.substringAfterLast('/'),
                            completedItems = extractedCount,
                            totalItems = totalCount,
                            percentage = if (totalCount > 0) extractedCount.toFloat() / totalCount else 0f
                        )
                    )

                    if (header.isDirectory) {
                        targetFile.mkdirs()
                        extractedCount++
                        continue
                    }

                    targetFile.parentFile?.mkdirs()

                    val finalFile = if (targetFile.exists()) {
                        when (conflictResolution) {
                            FileConflictResolution.SKIP -> {
                                extractedCount++
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

                    FileOutputStream(finalFile).use { out ->
                        rar.extractFile(header, out)
                    }

                    totalUncompressed += finalFile.length()
                    ArchiveSecurityPolicy.checkBombThresholds(
                        compressedBytesRead = header.packSize.coerceAtLeast(1L),
                        uncompressedBytesWritten = totalUncompressed,
                        singleEntryBytesWritten = finalFile.length()
                    )

                    if (header.mTime != null) {
                        finalFile.setLastModified(header.mTime.time)
                    }

                    extractedCount++
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
            ResultWrapper.Error(e, "Failed to extract RAR: ${e.message}")
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
