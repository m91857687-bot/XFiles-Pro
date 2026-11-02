package com.xfiles.pro.domain.model

/**
 * Event emitted when storage modifications occur (create, update, move, delete).
 */
sealed class StorageEvent {
    data class Created(val item: FileItem, val source: String = "STORAGE", val volumeId: String = "primary") : StorageEvent()
    data class Updated(val item: FileItem) : StorageEvent()
    data class Deleted(val path: String) : StorageEvent()
    data class Moved(val oldPath: String, val newItem: FileItem) : StorageEvent()
}
