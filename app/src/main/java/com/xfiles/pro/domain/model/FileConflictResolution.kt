package com.xfiles.pro.domain.model

/**
 * Strategy for resolving conflicts when a file or directory with the same name exists.
 */
enum class FileConflictResolution {
    OVERWRITE,
    SKIP,
    AUTO_RENAME
}
