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
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel as Zip4jCompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * High-performance ZIP archive handler using Zip4j.
 * Supports:
 * - Encryption (AES-256 and Standard)
 * - Compression levels
 * - Selective & full extraction with Zip Slip protection and Bomb detection
 * - In-place entry additions and deletions
 */
class ZipArchiveHandler {

    fun isEncrypted(archiveFile: File): Boolean {
        return try {
            val zipFile = ZipFile(archiveFile)
            zipFile.isEncrypted || zipFile.fileHeaders.any { it.isEncrypted }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listEntries(
        archiveFile: File,
        password: String? = null
    ): ResultWrapper<List<ArchiveEntryItem>> {
        return try {
            val zipFile = if (!password.isNullOrEmpty()) {
                ZipFile(archiveFile, password.toCharArray())
            } else {
                ZipFile(archiveFile)
            }

            if (!zipFile.isValidZipFile) {
                return ResultWrapper.Error(IllegalArgumentException("Invalid or corrupted ZIP archive: ${archiveFile.name}"), "Invalid or corrupted ZIP archive: ${archiveFile.name}")
            }

            val headers = zipFile.fileHeaders
            val items = headers.map { header ->
                val cleanPath = header.fileName.replace('\\', '/')
                val isDir = header.isDirectory || cleanPath.endsWith('/')
                val name = cleanPath.trimEnd('/').substringAfterLast('/')

                ArchiveEntryItem(
                    path = cleanPath,
                    name = if (name.isEmpty()) cleanPath else name,
                    isDirectory = isDir,
                    uncompressedSize = header.uncompressedSize,
                    compressedSize = header.compressedSize,
                    lastModified = header.lastModifiedTime,
                    isEncrypted = header.isEncrypted,
                    comment = header.fileComment
                )
            }
            ResultWrapper.Success(items)
        } catch (e: ZipException) {
            ResultWrapper.Error(e, "ZIP error: ${e.message}")
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to list ZIP entries: ${e.message}")
        }
    }

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationArchiveFile: File,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        password: String? = null,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<File> {
        return try {
            if (destinationArchiveFile.exists()) {
                destinationArchiveFile.delete()
            }
            destinationArchiveFile.parentFile?.mkdirs()

            val zipFile = if (!password.isNullOrEmpty()) {
                ZipFile(destinationArchiveFile, password.toCharArray())
            } else {
                ZipFile(destinationArchiveFile)
            }

            val zipParameters = ZipParameters().apply {
                if (compressionLevel == CompressionLevel.STORE) {
                    compressionMethod = CompressionMethod.STORE
                } else {
                    compressionMethod = CompressionMethod.DEFLATE
                    this.compressionLevel = when (compressionLevel) {
                        CompressionLevel.STORE -> Zip4jCompressionLevel.FASTEST
                        CompressionLevel.FAST -> Zip4jCompressionLevel.FAST
                        CompressionLevel.NORMAL -> Zip4jCompressionLevel.NORMAL
                        CompressionLevel.MAXIMUM -> Zip4jCompressionLevel.MAXIMUM
                    }
                }

                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }

            val totalFiles = sourceFiles.size
            var completed = 0

            for (source in sourceFiles) {
                currentCoroutineContext().ensureActive()

                onProgress(
                    ArchiveProgress(
                        currentItem = source.name,
                        completedItems = completed,
                        totalItems = totalFiles,
                        percentage = if (totalFiles > 0) completed.toFloat() / totalFiles else 0f
                    )
                )

                if (source.isDirectory) {
                    zipFile.addFolder(source, zipParameters)
                } else if (source.exists()) {
                    zipFile.addFile(source, zipParameters)
                }
                completed++
            }

            onProgress(
                ArchiveProgress(
                    currentItem = destinationArchiveFile.name,
                    completedItems = totalFiles,
                    totalItems = totalFiles,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(destinationArchiveFile)
        } catch (e: CancellationException) {
            destinationArchiveFile.delete()
            throw e
        } catch (e: Exception) {
            destinationArchiveFile.delete()
            ResultWrapper.Error(e, "Failed to create ZIP: ${e.message}")
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

            val zipFile = if (!password.isNullOrEmpty()) {
                ZipFile(archiveFile, password.toCharArray())
            } else {
                ZipFile(archiveFile)
            }

            if (!zipFile.isValidZipFile) {
                return ResultWrapper.Error(IllegalArgumentException("Invalid or corrupted ZIP archive: ${archiveFile.name}"), "Invalid or corrupted ZIP archive: ${archiveFile.name}")
            }

            val allHeaders = zipFile.fileHeaders
            val targetHeaders = if (selectedEntries != null && selectedEntries.isNotEmpty()) {
                val selectedSet = selectedEntries.map { it.replace('\\', '/') }.toSet()
                allHeaders.filter { header ->
                    val cleanPath = header.fileName.replace('\\', '/')
                    selectedSet.contains(cleanPath) || selectedSet.any { cleanPath.startsWith("$it/") }
                }
            } else {
                allHeaders
            }

            var extractedCount = 0
            val totalCount = targetHeaders.size
            var totalUncompressedWritten = 0L
            val buffer = ByteArray(ArchiveBufferConstants.BUFFER_SIZE)

            for (header in targetHeaders) {
                currentCoroutineContext().ensureActive()

                val cleanPath = header.fileName.replace('\\', '/')
                val targetFile = ArchiveSecurityPolicy.resolveAndValidateDestination(destinationDir, cleanPath)

                onProgress(
                    ArchiveProgress(
                        currentItem = cleanPath.substringAfterLast('/'),
                        completedItems = extractedCount,
                        totalItems = totalCount,
                        percentage = if (totalCount > 0) extractedCount.toFloat() / totalCount else 0f
                    )
                )

                if (header.isDirectory || cleanPath.endsWith('/')) {
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
                        FileConflictResolution.AUTO_RENAME -> {
                            resolveAutoRename(targetFile)
                        }
                    }
                } else {
                    targetFile
                }

                var entryBytesWritten = 0L
                zipFile.getInputStream(header).use { input ->
                    FileOutputStream(finalFile).use { output ->
                        var readBytes: Int
                        while (input.read(buffer).also { readBytes = it } != -1) {
                            output.write(buffer, 0, readBytes)
                            entryBytesWritten += readBytes
                            totalUncompressedWritten += readBytes
                            ArchiveSecurityPolicy.checkBombThresholds(
                                compressedBytesRead = header.compressedSize.coerceAtLeast(1L),
                                uncompressedBytesWritten = totalUncompressedWritten,
                                singleEntryBytesWritten = entryBytesWritten
                            )
                        }
                    }
                }

                if (header.lastModifiedTime > 0) {
                    finalFile.setLastModified(header.lastModifiedTime)
                }

                extractedCount++
            }

            onProgress(
                ArchiveProgress(
                    currentItem = "Completed",
                    completedItems = totalCount,
                    totalItems = totalCount,
                    percentage = 1.0f
                )
            )

            ResultWrapper.Success(extractedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to extract ZIP: ${e.message}")
        }
    }

    suspend fun deleteEntries(
        zipFile: File,
        entryPathsToDelete: List<String>
    ): ResultWrapper<Boolean> {
        return try {
            val zip = ZipFile(zipFile)
            for (path in entryPathsToDelete) {
                currentCoroutineContext().ensureActive()
                val cleanPath = path.replace('\\', '/')
                val header = zip.getFileHeader(cleanPath)
                    ?: zip.fileHeaders.firstOrNull { it.fileName.replace('\\', '/') == cleanPath }

                if (header != null) {
                    zip.removeFile(header)
                }
            }
            ResultWrapper.Success(true)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to delete entries from ZIP: ${e.message}")
        }
    }

    suspend fun addFiles(
        zipFile: File,
        filesToAdd: List<File>,
        targetDirectoryInsideArchive: String = "",
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Boolean> {
        return try {
            val zip = ZipFile(zipFile)
            val zipParameters = ZipParameters().apply {
                if (targetDirectoryInsideArchive.isNotBlank()) {
                    rootFolderNameInZip = targetDirectoryInsideArchive.trimEnd('/') + "/"
                }
            }

            var index = 0
            for (file in filesToAdd) {
                currentCoroutineContext().ensureActive()
                onProgress(
                    ArchiveProgress(
                        currentItem = file.name,
                        completedItems = index,
                        totalItems = filesToAdd.size,
                        percentage = if (filesToAdd.isNotEmpty()) index.toFloat() / filesToAdd.size else 0f
                    )
                )

                if (file.isDirectory) {
                    zip.addFolder(file, zipParameters)
                } else if (file.exists()) {
                    zip.addFile(file, zipParameters)
                }
                index++
            }

            ResultWrapper.Success(true)
        } catch (e: Exception) {
            ResultWrapper.Error(e, "Failed to add files to ZIP: ${e.message}")
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
