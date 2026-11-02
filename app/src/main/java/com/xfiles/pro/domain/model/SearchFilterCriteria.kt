package com.xfiles.pro.domain.model

/**
 * Encapsulates multi-criteria parameters for advanced file search.
 */
data class SearchFilterCriteria(
    val query: String = "",
    val fileType: FileType? = null,
    val sizeRange: SizeFilterRange = SizeFilterRange.ALL,
    val dateRange: DateFilterRange = DateFilterRange.ALL,
    val searchTarget: SearchTarget = SearchTarget.BOTH,
    val scopePath: String? = null, // Search within specific directory scope or null for all storage
    val extension: String? = null,
    val sortOption: FileSortOption = FileSortOption.NAME_ASC
) {
    val isFilteringActive: Boolean
        get() = fileType != null ||
                sizeRange != SizeFilterRange.ALL ||
                dateRange != DateFilterRange.ALL ||
                searchTarget != SearchTarget.BOTH ||
                !scopePath.isNullOrBlank() ||
                !extension.isNullOrBlank()
}
