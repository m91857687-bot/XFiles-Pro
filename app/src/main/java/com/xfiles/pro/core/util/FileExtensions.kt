package com.xfiles.pro.core.util

import com.xfiles.pro.domain.model.FileType
import java.io.File
import java.util.Locale

/**
 * File utility extensions for type detection and path handling.
 */
object FileExtensions {

    fun getExtension(nameOrPath: String): String {
        val lastDot = nameOrPath.lastIndexOf('.')
        val lastSeparator = nameOrPath.lastIndexOf(File.separatorChar)
        return if (lastDot > lastSeparator && lastDot != -1) {
            nameOrPath.substring(lastDot + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun detectFileType(extension: String, isDirectory: Boolean): FileType {
        if (isDirectory) return FileType.FOLDER

        return when (extension.lowercase(Locale.ROOT)) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif" -> FileType.IMAGE
            "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "ts" -> FileType.VIDEO
            "mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "wma" -> FileType.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "md" -> FileType.DOCUMENT
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> FileType.ARCHIVE
            "apk", "apks", "xapk", "apkm" -> FileType.APK
            else -> FileType.UNKNOWN
        }
    }

    fun getMimeType(extension: String): String {
        return when (extension.lowercase(Locale.ROOT)) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }
}
