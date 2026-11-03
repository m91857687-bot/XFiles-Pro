package com.xfiles.pro.data

import com.xfiles.pro.data.local.download.DownloadFileNameExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadFileNameExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `extracts filename from standard Content-Disposition header`() {
        val header = "attachment; filename=\"report_2026.pdf\""
        val name = DownloadFileNameExtractor.extractFileName(
            url = "https://example.com/download?id=123",
            contentDispositionHeader = header
        )
        assertEquals("report_2026.pdf", name)
    }

    @Test
    fun `extracts filename from unquoted Content-Disposition header`() {
        val header = "attachment; filename=document.docx"
        val name = DownloadFileNameExtractor.extractFileName(
            url = "https://example.com/download",
            contentDispositionHeader = header
        )
        assertEquals("document.docx", name)
    }

    @Test
    fun `extracts filename from RFC 5987 UTF-8 encoded Content-Disposition header`() {
        val header = "attachment; filename*=UTF-8''%D9%85%D9%84%D9%81_%D8%AA%D8%AC%D8%B1%D8%A8%D8%A9.zip"
        val name = DownloadFileNameExtractor.extractFileName(
            url = "https://example.com/file",
            contentDispositionHeader = header
        )
        assertEquals("ملف_تجربة.zip", name)
    }

    @Test
    fun `extracts filename from URL path when header is missing`() {
        val url = "https://cdn.example.com/releases/app-v2.5.apk?token=xyz#section"
        val name = DownloadFileNameExtractor.extractFileName(url)
        assertEquals("app-v2.5.apk", name)
    }

    @Test
    fun `sanitizes path traversal characters from filename`() {
        val dangerousName = "../../../etc/passwd"
        val sanitized = DownloadFileNameExtractor.sanitize(dangerousName)
        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
        assertEquals("etcpasswd", sanitized)
    }

    @Test
    fun `replaces illegal characters with underscores`() {
        val badName = "my*file:name?<test>|.tar.gz"
        val sanitized = DownloadFileNameExtractor.sanitize(badName)
        assertEquals("my_file_name__test__.tar.gz", sanitized)
    }

    @Test
    fun `suggested name takes precedence when valid`() {
        val name = DownloadFileNameExtractor.extractFileName(
            url = "https://example.com/download.zip",
            contentDispositionHeader = "attachment; filename=\"original.zip\"",
            suggestedName = "custom_archive.zip"
        )
        assertEquals("custom_archive.zip", name)
    }

    @Test
    fun `resolves unique filename when file already exists`() {
        val dir = tempFolder.newFolder("downloads")
        File(dir, "archive.zip").writeText("existing")

        val unique1 = DownloadFileNameExtractor.resolveUniqueFileName(dir, "archive.zip")
        assertEquals("archive (1).zip", unique1)

        File(dir, "archive (1).zip").writeText("existing 1")
        val unique2 = DownloadFileNameExtractor.resolveUniqueFileName(dir, "archive.zip")
        assertEquals("archive (2).zip", unique2)
    }
}
