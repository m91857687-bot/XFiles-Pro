package com.xfiles.pro.domain.model

import java.util.concurrent.TimeUnit

/**
 * Filter criteria for file modification dates in advanced search.
 */
enum class DateFilterRange {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR;

    fun getCutoffTimestamp(currentTimeMillis: Long = System.currentTimeMillis()): Long {
        return when (this) {
            ALL -> 0L
            TODAY -> currentTimeMillis - TimeUnit.DAYS.toMillis(1)
            THIS_WEEK -> currentTimeMillis - TimeUnit.DAYS.toMillis(7)
            THIS_MONTH -> currentTimeMillis - TimeUnit.DAYS.toMillis(30)
            THIS_YEAR -> currentTimeMillis - TimeUnit.DAYS.toMillis(365)
        }
    }

    fun matches(lastModified: Long, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (this == ALL) return true
        val cutoff = getCutoffTimestamp(currentTimeMillis)
        return lastModified >= cutoff
    }
}
