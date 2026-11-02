package com.xfiles.pro.data.local.archive

/**
 * Standard buffer sizes for high-performance streaming I/O during compression/decompression.
 */
object ArchiveBufferConstants {
    const val BUFFER_SIZE: Int = 64 * 1024 // 64 KB streaming buffer
}
