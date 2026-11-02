package com.xfiles.pro.domain.model

import java.io.File
import java.util.Locale

/**
 * Supported archive and compression formats in XFiles Pro.
 * Defines capabilities of each format based on underlying library support.
 */
enum class ArchiveFormat(
    val extension: String,
    val supportsCreation: Boolean,
    val supportsExtraction: Boolean,
    val supportsPassword: Boolean,
    val supportsInPlaceModification: Boolean,
    val displayName: String
) {
    ZIP(
        extension = "zip",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = true,
        supportsInPlaceModification = true,
        displayName = "ZIP Archive (.zip)"
    ),
    TAR(
        extension = "tar",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "TAR Archive (.tar)"
    ),
    TAR_GZ(
        extension = "tar.gz",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "GZipped TAR (.tar.gz)"
    ),
    TGZ(
        extension = "tgz",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "TGZ Archive (.tgz)"
    ),
    GZIP(
        extension = "gz",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "GZIP Compressed (.gz)"
    ),
    BZIP2(
        extension = "bz2",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "BZIP2 Compressed (.bz2)"
    ),
    XZ(
        extension = "xz",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "XZ Compressed (.xz)"
    ),
    SEVEN_Z(
        extension = "7z",
        supportsCreation = true,
        supportsExtraction = true,
        supportsPassword = false,
        supportsInPlaceModification = false,
        displayName = "7-Zip Archive (.7z)"
    ),
    RAR(
        extension = "rar",
        supportsCreation = false, // Proprietary format: extraction & browsing only
        supportsExtraction = true,
        supportsPassword = true,
        supportsInPlaceModification = false,
        displayName = "RAR Archive (.rar)"
    );

    companion object {
        fun detectFormat(fileNameOrPath: String): ArchiveFormat? {
            val lower = fileNameOrPath.lowercase(Locale.ROOT)
            return when {
                lower.endsWith(".tar.gz") -> TAR_GZ
                lower.endsWith(".tgz") -> TGZ
                lower.endsWith(".tar") -> TAR
                lower.endsWith(".zip") -> ZIP
                lower.endsWith(".7z") -> SEVEN_Z
                lower.endsWith(".rar") -> RAR
                lower.endsWith(".gz") -> GZIP
                lower.endsWith(".bz2") -> BZIP2
                lower.endsWith(".xz") -> XZ
                else -> null
            }
        }

        fun fromExtension(ext: String): ArchiveFormat? {
            val cleanExt = ext.removePrefix(".").lowercase(Locale.ROOT)
            return entries.firstOrNull { it.extension == cleanExt }
        }
    }
}
