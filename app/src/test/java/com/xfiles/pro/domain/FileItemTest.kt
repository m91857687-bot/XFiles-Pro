package com.xfiles.pro.domain

import com.xfiles.pro.core.util.FileExtensions
import com.xfiles.pro.domain.model.FileItem
import com.xfiles.pro.domain.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileItemTest {

    @Test
    fun `detectFileType resolves correctly for extensions`() {
        assertEquals(FileType.IMAGE, FileExtensions.detectFileType("png", isDirectory = false))
        assertEquals(FileType.VIDEO, FileExtensions.detectFileType("mp4", isDirectory = false))
        assertEquals(FileType.AUDIO, FileExtensions.detectFileType("mp3", isDirectory = false))
        assertEquals(FileType.DOCUMENT, FileExtensions.detectFileType("pdf", isDirectory = false))
        assertEquals(FileType.ARCHIVE, FileExtensions.detectFileType("zip", isDirectory = false))
        assertEquals(FileType.APK, FileExtensions.detectFileType("apk", isDirectory = false))
        assertEquals(FileType.FOLDER, FileExtensions.detectFileType("", isDirectory = true))
        assertEquals(FileType.UNKNOWN, FileExtensions.detectFileType("xyz123", isDirectory = false))
    }

    @Test
    fun `fileItem properties are preserved`() {
        val item = FileItem(
            name = "document.pdf",
            path = "/storage/emulated/0/document.pdf",
            size = 2048L,
            lastModified = 1000L,
            isDirectory = false,
            fileType = FileType.DOCUMENT,
            extension = "pdf",
            isHidden = false
        )

        assertEquals("document.pdf", item.name)
        assertEquals(2048L, item.size)
        assertFalse(item.isDirectory)
        assertFalse(item.isHidden)
        assertTrue(item.canRead)
    }
}
