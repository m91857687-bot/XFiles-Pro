package com.xfiles.pro.domain.model

/**
 * Exceptions thrown when an archive violates security constraints
 * (e.g., Zip Slip, Path Traversal, Archive Bomb, Symlink escaping target directory).
 */
sealed class ArchiveSecurityException(message: String) : SecurityException(message)

class ZipSlipException(val entryPath: String) :
    ArchiveSecurityException("Zip Slip path traversal attempt detected for entry: '$entryPath'")

class ArchiveBombException(val ratio: Double, val totalUncompressedBytes: Long) :
    ArchiveSecurityException("Archive bomb detected (ratio: ${String.format("%.1f", ratio)}:1, uncompressed bytes: $totalUncompressedBytes)")

class SymlinkTraversalException(val entryPath: String, val targetPath: String) :
    ArchiveSecurityException("Symbolic link in '$entryPath' points outside destination boundary: '$targetPath'")
