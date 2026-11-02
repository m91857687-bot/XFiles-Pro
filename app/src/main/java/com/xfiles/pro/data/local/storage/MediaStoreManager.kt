package com.xfiles.pro.data.local.storage

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType

/**
 * Manages access to Android MediaStore API for reading indexed media, documents, and downloads.
 */
class MediaStoreManager(private val context: Context) {

    private val contentResolver get() = context.contentResolver

    /**
     * Queries files from MediaStore matching the given media category.
     */
    fun queryMedia(fileType: FileType, limit: Int = 100): List<FileItem> {
        val collection = when (fileType) {
            FileType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            FileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            FileType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }

        return queryCollection(collection, limit = limit)
    }

    /**
     * Queries the most recently modified files across MediaStore.
     */
    fun queryRecentFiles(limit: Int = 20): List<FileItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC LIMIT $limit"
        return queryCollection(collection, sortOrder = sortOrder, limit = limit)
    }

    private fun queryCollection(
        collection: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        sortOrder: String = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        limit: Int = 100
    ): List<FileItem> {
        val fileItems = mutableListOf<FileItem>()

        @Suppress("DEPRECATION")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATA
        )

        try {
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                @Suppress("DEPRECATION")
                val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = if (idColumn >= 0) cursor.getLong(idColumn) else -1L
                    val name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: "Unnamed" else "Unnamed"
                    val size = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else 0L
                    val dateModified = if (dateColumn >= 0) cursor.getLong(dateColumn) * 1000L else 0L

                    val itemUri = ContentUris.withAppendedId(collection, id)
                    val rawFilePath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val resolvedPath = if (!rawFilePath.isNullOrBlank()) rawFilePath else itemUri.toString()
                    val ext = FileExtensions.getExtension(name)
                    val detectedType = FileExtensions.detectFileType(ext, isDirectory = false)

                    fileItems.add(
                        FileItem(
                            name = name,
                            path = resolvedPath,
                            uriString = itemUri.toString(),
                            size = size,
                            lastModified = dateModified,
                            isDirectory = false,
                            fileType = detectedType,
                            extension = ext,
                            isHidden = name.startsWith("."),
                            canRead = true,
                            canWrite = false
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }

        return fileItems
    }
}
