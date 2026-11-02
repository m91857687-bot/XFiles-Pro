package com.xfiles.pro.core

import com.xfiles.pro.core.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun `format zero bytes returns 0 B`() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("0 B", ByteFormatter.format(-50L))
    }

    @Test
    fun `format kilobytes correctly`() {
        assertEquals("1.0 KB", ByteFormatter.format(1024L))
        assertEquals("2.5 KB", ByteFormatter.format(2560L))
    }

    @Test
    fun `format megabytes and gigabytes correctly`() {
        assertEquals("1.0 MB", ByteFormatter.format(1024L * 1024L))
        assertEquals("1.0 GB", ByteFormatter.format(1024L * 1024L * 1024L))
    }
}
