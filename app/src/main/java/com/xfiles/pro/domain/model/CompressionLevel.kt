package com.xfiles.pro.domain.model

/**
 * Compression level setting for archive creation.
 */
enum class CompressionLevel(val level: Int, val displayName: String) {
    STORE(0, "Store (No compression - Fastest)"),
    FAST(1, "Fast"),
    NORMAL(6, "Normal (Balanced)"),
    MAXIMUM(9, "Maximum (Smallest size - Slower)")
}
