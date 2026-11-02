package com.xfiles.pro.data.local.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.SafPermission
import java.io.InputStream
import java.io.OutputStream

/**
 * Manages Storage Access Framework (SAF) URI permissions and native DocumentsContract operations.
 */
class SafManager(private val context: Context) {

    private val contentResolver get() = context.contentResolver

    /**
     * Persists read and write permissions for a chosen Document Tree URI.
     */
    fun takePersistablePermission(treeUri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(treeUri, flags)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Releases a previously persisted Document Tree URI permission.
     */
    fun releasePersistablePermission(treeUri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.releasePersistableUriPermission(treeUri, flags)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Retrieves all currently valid, persisted URI permissions.
     */
    fun getPersistedPermissions(): List<SafPermission> {
        return try {
            contentResolver.persistedUriPermissions.map { permission ->
                SafPermission(
                    uriString = permission.uri.toString(),
                    isReadGranted = permission.isReadPermission,
                    isWriteGranted = permission.isWritePermission,
                    persistedTime = permission.persistedTime
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Checks if a tree URI has active, non-revoked read and write permissions.
     */
    fun isPermissionGranted(uri: Uri): Boolean {
        return try {
            contentResolver.persistedUriPermissions.any { perm ->
                perm.uri == uri && perm.isReadPermission && perm.isWritePermission
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries child documents within a Document Tree URI using native ContentResolver.
     */
    fun listDocumentFiles(treeUri: Uri): List<FileItem> {
        val fileItems = mutableListOf<FileItem>()
        return try {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_FLAGS
            )

            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val flagsCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)

                while (cursor.moveToNext()) {
                    val childDocId = if (idCol >= 0) cursor.getString(idCol) else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unnamed" else "Unnamed"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val lastModified = if (modifiedCol >= 0) cursor.getLong(modifiedCol) else 0L
                    val flags = if (flagsCol >= 0) cursor.getInt(flagsCol) else 0

                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                    val ext = if (isDir) "" else FileExtensions.getExtension(name)
                    val type = FileExtensions.detectFileType(ext, isDir)

                    val canWrite = (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0 ||
                            (flags and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE) != 0

                    fileItems.add(
                        FileItem(
                            name = name,
                            path = childUri.toString(),
                            uriString = childUri.toString(),
                            size = if (isDir) 0L else size,
                            lastModified = lastModified,
                            isDirectory = isDir,
                            fileType = type,
                            extension = ext,
                            isHidden = name.startsWith("."),
                            canRead = true,
                            canWrite = canWrite
                        )
                    )
                }
            }
            fileItems
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Creates a new directory under the specified parent document URI.
     */
    fun createDirectory(parentUri: Uri, name: String): Uri? {
        return try {
            DocumentsContract.createDocument(
                contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a new file under the specified parent document URI.
     */
    fun createFile(parentUri: Uri, name: String, mimeType: String): Uri? {
        return try {
            DocumentsContract.createDocument(
                contentResolver,
                parentUri,
                mimeType,
                name
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a document using DocumentsContract.
     */
    fun deleteDocument(documentUri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(contentResolver, documentUri)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Renames a document using DocumentsContract.
     */
    fun renameDocument(documentUri: Uri, newName: String): Uri? {
        return try {
            DocumentsContract.renameDocument(contentResolver, documentUri, newName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens an input stream for reading document data.
     */
    fun openInputStream(uri: Uri): InputStream? {
        return contentResolver.openInputStream(uri)
    }

    /**
     * Opens an output stream for writing document data.
     */
    fun openOutputStream(uri: Uri, mode: String = "wt"): OutputStream? {
        return contentResolver.openOutputStream(uri, mode)
    }
}
