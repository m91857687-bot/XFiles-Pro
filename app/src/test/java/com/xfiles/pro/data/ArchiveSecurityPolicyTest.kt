package com.xfiles.pro.data

import com.xfiles.pro.domain.model.ArchiveBombException
import com.xfiles.pro.domain.model.ArchiveSecurityPolicy
import com.xfiles.pro.domain.model.ZipSlipException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArchiveSecurityPolicyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun resolveAndValidateDestination_validRelativePath_resolvesSafely() {
        val dest = tempFolder.newFolder("output")
        val file = ArchiveSecurityPolicy.resolveAndValidateDestination(dest, "folder/subfolder/file.txt")

        assertNotNull(file)
        assertTrue(file.canonicalPath.startsWith(dest.canonicalPath))
        assertEquals("file.txt", file.name)
    }

    @Test
    fun resolveAndValidateDestination_zipSlipAttempt_throwsZipSlipException() {
        val dest = tempFolder.newFolder("output")

        try {
            ArchiveSecurityPolicy.resolveAndValidateDestination(dest, "../../escape.txt")
            fail("Expected ZipSlipException was not thrown")
        } catch (e: ZipSlipException) {
            assertTrue(e.message?.contains("Zip Slip") == true)
        }
    }

    @Test
    fun resolveAndValidateDestination_deepZipSlipAttempt_throwsZipSlipException() {
        val dest = tempFolder.newFolder("output")

        try {
            ArchiveSecurityPolicy.resolveAndValidateDestination(dest, "a/b/../../../../evil.sh")
            fail("Expected ZipSlipException was not thrown")
        } catch (e: ZipSlipException) {
            assertTrue(e.message?.contains("Zip Slip") == true)
        }
    }

    @Test
    fun checkBombThresholds_normalRatio_passes() {
        // 1MB compressed, 5MB uncompressed (5:1 ratio)
        ArchiveSecurityPolicy.checkBombThresholds(
            compressedBytesRead = 1024 * 1024,
            uncompressedBytesWritten = 5 * 1024 * 1024,
            singleEntryBytesWritten = 5 * 1024 * 1024
        )
    }

    @Test
    fun checkBombThresholds_excessiveRatio_throwsArchiveBombException() {
        // 10KB compressed, 50MB uncompressed (5000:1 ratio)
        try {
            ArchiveSecurityPolicy.checkBombThresholds(
                compressedBytesRead = 10 * 1024,
                uncompressedBytesWritten = 50 * 1024 * 1024,
                singleEntryBytesWritten = 50 * 1024 * 1024
            )
            fail("Expected ArchiveBombException was not thrown")
        } catch (e: ArchiveBombException) {
            assertTrue(e.message?.contains("bomb") == true)
        }
    }
}
