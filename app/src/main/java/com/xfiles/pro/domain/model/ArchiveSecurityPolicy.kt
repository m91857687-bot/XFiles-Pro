package com.xfiles.pro.domain.model

import java.io.File
import java.io.IOException

/**
 * Security policy enforcing safety against malicious archive attacks:
 * - Zip Slip / Path Traversal: Blocks entry paths attempting directory traversal (e.g. "../../../etc/passwd").
 * - Archive Bombs: Guards against excessive uncompressed size and extreme compression ratios.
 * - Symlink Escapes: Ensures symlink destinations remain strictly inside the output folder.
 */
object ArchiveSecurityPolicy {

    const val MAX_EXPANSION_RATIO: Double = 100.0
    const val MAX_TOTAL_UNCOMPRESSED_BYTES: Long = 5L * 1024 * 1024 * 1024 // 5 GB
    const val MAX_SINGLE_ENTRY_UNCOMPRESSED_BYTES: Long = 2L * 1024 * 1024 * 1024 // 2 GB

    /**
     * Validates and resolves the target file within the destination directory.
     * Throws [ZipSlipException] if the resolved path escapes [destinationDir].
     */
    @Throws(ZipSlipException::class, IOException::class)
    fun resolveAndValidateDestination(destinationDir: File, entryPath: String): File {
        // Normalize slashes
        val normalized = entryPath.replace('\\', '/')
        val cleanEntry = if (normalized.startsWith("/")) normalized.substring(1) else normalized

        val destinationCanonical = destinationDir.canonicalFile
        val destinationCanonicalPath = destinationCanonical.path

        val targetFile = File(destinationCanonical, cleanEntry)
        val targetCanonicalPath = targetFile.canonicalPath

        // Target must strictly start with destination path plus directory separator (or be equal for root)
        val isSafe = targetCanonicalPath == destinationCanonicalPath ||
                targetCanonicalPath.startsWith(destinationCanonicalPath + File.separator)

        if (!isSafe) {
            throw ZipSlipException(entryPath)
        }

        return targetFile
    }

    /**
     * Checks whether extracted size or compression ratio exceeds safe thresholds.
     */
    @Throws(ArchiveBombException::class)
    fun checkBombThresholds(
        compressedBytesRead: Long,
        uncompressedBytesWritten: Long,
        singleEntryBytesWritten: Long = 0L
    ) {
        if (singleEntryBytesWritten > MAX_SINGLE_ENTRY_UNCOMPRESSED_BYTES) {
            throw ArchiveBombException(
                ratio = if (compressedBytesRead > 0) singleEntryBytesWritten.toDouble() / compressedBytesRead else 1000.0,
                totalUncompressedBytes = singleEntryBytesWritten
            )
        }

        if (uncompressedBytesWritten > MAX_TOTAL_UNCOMPRESSED_BYTES) {
            throw ArchiveBombException(
                ratio = if (compressedBytesRead > 0) uncompressedBytesWritten.toDouble() / compressedBytesRead else 1000.0,
                totalUncompressedBytes = uncompressedBytesWritten
            )
        }

        // Only enforce ratio check once substantial data is written (> 1MB) to prevent false positives on tiny headers
        if (uncompressedBytesWritten > 1024 * 1024 && compressedBytesRead > 0) {
            val ratio = uncompressedBytesWritten.toDouble() / compressedBytesRead
            if (ratio > MAX_EXPANSION_RATIO) {
                throw ArchiveBombException(ratio, uncompressedBytesWritten)
            }
        }
    }
}
