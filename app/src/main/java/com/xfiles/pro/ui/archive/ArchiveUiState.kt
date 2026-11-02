package com.xfiles.pro.ui.archive

import com.xfiles.pro.domain.model.ArchiveEntryItem
import com.xfiles.pro.domain.model.ArchiveFormat
import com.xfiles.pro.domain.model.ArchiveProgress
import java.io.File

/**
 * UI State for the Archive Viewer screen and operations.
 */
data class ArchiveUiState(
    val archiveFile: File? = null,
    val format: ArchiveFormat? = null,
    val currentVirtualPath: String = "",
    val allEntries: List<ArchiveEntryItem> = emptyList(),
    val visibleEntries: List<ArchiveEntryItem> = emptyList(),
    val selectedEntryPaths: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isEncrypted: Boolean = false,
    val password: String? = null,
    val showPasswordPrompt: Boolean = false,
    val showExtractDialog: Boolean = false,
    val isOperationInProgress: Boolean = false,
    val progress: ArchiveProgress? = null,
    val operationSuccessMessage: String? = null,
    val errorMessage: String? = null
) {
    val totalUncompressedSize: Long
        get() = allEntries.filter { !it.isDirectory }.sumOf { it.uncompressedSize }

    val totalCompressedSize: Long
        get() = archiveFile?.length() ?: 0L

    val isVirtualRoot: Boolean
        get() = currentVirtualPath.isEmpty()

    val virtualBreadcrumbs: List<Pair<String, String>>
        get() {
            if (currentVirtualPath.isEmpty()) return listOf("/" to "")
            val parts = currentVirtualPath.split('/').filter { it.isNotEmpty() }
            val crumbs = mutableListOf("/" to "")
            var running = ""
            for (p in parts) {
                running = if (running.isEmpty()) p else "$running/$p"
                crumbs.add(p to running)
            }
            return crumbs
        }
}
