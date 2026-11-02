package com.xfiles.pro.domain.model

/**
 * Filter criteria for file sizes in advanced search.
 */
enum class SizeFilterRange(val minBytes: Long, val maxBytes: Long) {
    ALL(0L, Long.MAX_VALUE),
    TINY_UNDER_1MB(0L, 1024L * 1024L),
    MEDIUM_1_50MB(1024L * 1024L, 50L * 1024L * 1024L),
    LARGE_50_500MB(50L * 1024L * 1024L, 500L * 1024L * 1024L),
    HUGE_OVER_500MB(500L * 1024L * 1024L, Long.MAX_VALUE);

    fun matches(size: Long): Boolean = size in minBytes..maxBytes
}
