package com.xfiles.pro.domain.model

/**
 * Filter specifying target item type in search results.
 */
enum class SearchTarget {
    BOTH,
    FILES_ONLY,
    FOLDERS_ONLY;

    fun matches(isDirectory: Boolean): Boolean = when (this) {
        BOTH -> true
        FILES_ONLY -> !isDirectory
        FOLDERS_ONLY -> isDirectory
    }
}
