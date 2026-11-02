package com.xfiles.pro.core.util

import java.util.Locale

/**
 * Utility for formatting byte counts into human-readable strings (B, KB, MB, GB, TB).
 */
object ByteFormatter {
    private const val UNIT = 1024.0

    fun format(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < UNIT) return "$bytes B"

        val exp = (Math.log(bytes.toDouble()) / Math.log(UNIT)).toInt()
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val boundedExp = exp.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(UNIT, boundedExp.toDouble())

        return String.format(Locale.US, "%.1f %s", value, units[boundedExp])
    }
}
