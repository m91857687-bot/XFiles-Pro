package com.xfiles.pro.data.repository

import com.xfiles.pro.core.dispatcher.DispatchersProvider
import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.data.local.archive.RarArchiveHandler
import com.xfiles.pro.data.local.archive.SevenZArchiveHandler
import com.xfiles.pro.data.local.archive.SingleCompressorHandler
import com.xfiles.pro.data.local.archive.TarArchiveHandler
import com.xfiles.pro.data.local.archive.ZipArchiveHandler
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import com.xfiles.pro.domain.repository.ArchiveRepository
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Concrete implementation of [ArchiveRepository] routing archive operations to
 * specialized handlers on IO dispatchers.
 */
class ArchiveRepositoryImpl(
    private val dispatchers: DispatchersProvider,
    private val zipHandler: ZipArchiveHandler = ZipArchiveHandler(),
    private val tarHandler: TarArchiveHandler = TarArchiveHandler(),
    private val singleHandler: SingleCompressorHandler = SingleCompressorHandler(),
    private val sevenZHandler: SevenZArchiveHandler = SevenZArchiveHandler(),
    private val rarHandler: RarArchiveHandler = RarArchiveHandler()
) : ArchiveRepository {

    override suspend fun listEntries(
        archiveFile: File,
        password: String?
    ): ResultWrapper<List<ArchiveEntryItem>> = withContext(dispatchers.io) {
        if (!archiveFile.exists() || !archiveFile.isFile) {
            return@withContext ResultWrapper.Error(
                IllegalArgumentException("Archive file not found: ${archiveFile.path}"),
                "Archive file not found: ${archiveFile.path}"
            )
        }

        val format = ArchiveFormat.detectFormat(archiveFile.name)
            ?: return@withContext ResultWrapper.Error(
                IllegalArgumentException("Unsupported archive format for file: ${archiveFile.name}"),
                "Unsupported archive format for file: ${archiveFile.name}"
            )

        when (format) {
            ArchiveFormat.ZIP -> zipHandler.listEntries(archiveFile, password)
            ArchiveFormat.TAR -> tarHandler.listEntries(archiveFile, isGzip = false)
            ArchiveFormat.TAR_GZ, ArchiveFormat.TGZ -> tarHandler.listEntries(archiveFile, isGzip = true)
            ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ -> singleHandler.listEntries(archiveFile, format)
            ArchiveFormat.SEVEN_Z -> sevenZHandler.listEntries(archiveFile)
            ArchiveFormat.RAR -> rarHandler.listEntries(archiveFile, password)
        }
    }

    override suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
        password: String?,
        selectedEntries: List<String>?,
        conflictResolution: FileConflictResolution,
        onProgress: (ArchiveProgress) -> Unit
    ): ResultWrapper<Int> = withContext(dispatchers.io) {
        if (!archiveFile.exists() || !archiveFile.isFile) {
            return@withContext ResultWrapper.Error(
                IllegalArgumentException("Archive file not found: ${archiveFile.path}"),
                "Archive file not found: ${archiveFile.path}"
            )
        }

        val format = ArchiveFormat.detectFormat(archiveFile.name)
            ?: return@withContext ResultWrapper.Error(
                IllegalArgumentException("Unsupported archive format for file: ${archiveFile.name}"),
                "Unsupported archive format for file: ${archiveFile.name}"
            )

        when (format) {
            ArchiveFormat.ZIP -> zipHandler.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                password = password,
                selectedEntries = selectedEntries,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
            ArchiveFormat.TAR -> tarHandler.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                isGzip = false,
                selectedEntries = selectedEntries,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
            ArchiveFormat.TAR_GZ, ArchiveFormat.TGZ -> tarHandler.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                isGzip = true,
                selectedEntries = selectedEntries,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
            ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ -> singleHandler.decompressFile(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                format = format,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
            ArchiveFormat.SEVEN_Z -> sevenZHandler.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                selectedEntries = selectedEntries,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
            ArchiveFormat.RAR -> rarHandler.extractArchive(
                archiveFile = archiveFile,
                destinationDir = destinationDir,
                password = password,
                selectedEntries = selectedEntries,
                conflictResolution = conflictResolution,
                onProgress = onProgress
            )
        }
    }

    override suspend fun createArchive(
        sourceFiles: List<File>,
        destinationArchiveFile: File,
        format: ArchiveFormat,
        compressionLevel: CompressionLevel,
        password: String?,
        onProgress: (ArchiveProgress) -> Unit
    ): ResultWrapper<File> = withContext(dispatchers.io) {
        if (sourceFiles.isEmpty()) {
            return@withContext ResultWrapper.Error(
                IllegalArgumentException("No source files specified for archive creation"),
                "No source files specified for archive creation"
            )
        }

        if (!format.supportsCreation) {
            return@withContext ResultWrapper.Error(
                UnsupportedOperationException("Creating ${format.displayName} archives is not supported"),
                "Creating ${format.displayName} archives is not supported"
            )
        }

        when (format) {
            ArchiveFormat.ZIP -> zipHandler.createArchive(
                sourceFiles = sourceFiles,
                destinationArchiveFile = destinationArchiveFile,
                compressionLevel = compressionLevel,
                password = password,
                onProgress = onProgress
            )
            ArchiveFormat.TAR -> tarHandler.createArchive(
                sourceFiles = sourceFiles,
                destinationFile = destinationArchiveFile,
                isGzip = false,
                onProgress = onProgress
            )
            ArchiveFormat.TAR_GZ, ArchiveFormat.TGZ -> tarHandler.createArchive(
                sourceFiles = sourceFiles,
                destinationFile = destinationArchiveFile,
                isGzip = true,
                onProgress = onProgress
            )
            ArchiveFormat.GZIP, ArchiveFormat.BZIP2, ArchiveFormat.XZ -> {
                if (sourceFiles.size > 1 || sourceFiles.first().isDirectory) {
                    return@withContext ResultWrapper.Error(
                        IllegalArgumentException("${format.displayName} supports compressing only a single file. Use .tar.${format.extension} for multiple files or folders."),
                        "${format.displayName} supports compressing only a single file. Use .tar.${format.extension} for multiple files or folders."
                    )
                }
                singleHandler.compressFile(
                    sourceFile = sourceFiles.first(),
                    destinationFile = destinationArchiveFile,
                    format = format,
                    onProgress = onProgress
                )
            }
            ArchiveFormat.SEVEN_Z -> sevenZHandler.createArchive(
                sourceFiles = sourceFiles,
                destinationFile = destinationArchiveFile,
                compressionLevel = compressionLevel,
                onProgress = onProgress
            )
            ArchiveFormat.RAR -> ResultWrapper.Error(
                UnsupportedOperationException("Creation of RAR archives is proprietary and unsupported"),
                "Creation of RAR archives is proprietary and unsupported"
            )
        }
    }

    override suspend fun deleteEntriesFromZip(
        zipFile: File,
        entryPathsToDelete: List<String>
    ): ResultWrapper<Boolean> = withContext(dispatchers.io) {
        zipHandler.deleteEntries(zipFile, entryPathsToDelete)
    }

    override suspend fun addFilesToZip(
        zipFile: File,
        filesToAdd: List<File>,
        targetDirectoryInsideArchive: String,
        onProgress: (ArchiveProgress) -> Unit
    ): ResultWrapper<Boolean> = withContext(dispatchers.io) {
        zipHandler.addFiles(zipFile, filesToAdd, targetDirectoryInsideArchive, onProgress)
    }

    override suspend fun isPasswordProtected(archiveFile: File): Boolean = withContext(dispatchers.io) {
        when (ArchiveFormat.detectFormat(archiveFile.name)) {
            ArchiveFormat.ZIP -> zipHandler.isEncrypted(archiveFile)
            ArchiveFormat.RAR -> rarHandler.isEncrypted(archiveFile)
            else -> false
        }
    }
}
