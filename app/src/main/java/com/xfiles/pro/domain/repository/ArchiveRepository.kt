package com.xfiles.pro.domain.repository

import com.xfiles.pro.core.result.ResultWrapper
import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import com.xfiles.pro.domain.model.CompressionLevel
import com.xfiles.pro.domain.model.FileConflictResolution
import java.io.File

/**
 * Domain contract for archive operations:
 * - Listing archive contents (virtual browsing)
 * - Full or selective extraction
 * - Archive creation with compression levels & encryption
 * - In-place entry additions and removals (where supported by format)
 * - Password detection
 */
interface ArchiveRepository {

    /**
     * Lists all entries inside an archive file without decompressing payload.
     */
    suspend fun listEntries(
        archiveFile: File,
        password: String? = null
    ): ResultWrapper<List<ArchiveEntryItem>>

    /**
     * Extracts an archive (or selected entries) to the destination directory.
     * Returns the count of extracted files.
     */
    suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
        password: String? = null,
        selectedEntries: List<String>? = null,
        conflictResolution: FileConflictResolution = FileConflictResolution.AUTO_RENAME,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Int>

    /**
     * Creates an archive containing [sourceFiles] at [destinationArchiveFile].
     */
    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationArchiveFile: File,
        format: ArchiveFormat,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        password: String? = null,
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<File>

    /**
     * Deletes specified entries from a ZIP archive.
     */
    suspend fun deleteEntriesFromZip(
        zipFile: File,
        entryPathsToDelete: List<String>
    ): ResultWrapper<Boolean>

    /**
     * Adds files into an existing ZIP archive.
     */
    suspend fun addFilesToZip(
        zipFile: File,
        filesToAdd: List<File>,
        targetDirectoryInsideArchive: String = "",
        onProgress: (ArchiveProgress) -> Unit = {}
    ): ResultWrapper<Boolean>

    /**
     * Checks if the archive is encrypted / requires a password.
     */
    suspend fun isPasswordProtected(archiveFile: File): Boolean
}
