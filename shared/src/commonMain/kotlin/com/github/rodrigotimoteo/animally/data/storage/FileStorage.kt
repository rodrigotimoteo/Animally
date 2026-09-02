package com.github.rodrigotimoteo.animally.data.storage

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Platform-backed storage for files attached to patient records.
 *
 * Files are saved to the app-private storage directory (`filesDir/attachments`
 * on Android, `Documents/attachments` on iOS) so no runtime permissions are
 * required.
 */
expect object FileStorage {
    /**
     * Saves [bytes] to the app's private storage using [fileName] as display
     * metadata and returns the absolute path of the written file.
     */
    fun saveBytes(
        fileName: String,
        bytes: ByteArray,
    ): String

    /**
     * Deletes an app-owned file at [path]. Returns `true` when a file was
     * removed. Implementations must reject paths outside their private file
     * area.
     */
    fun delete(path: String): Boolean
}

/**
 * Splits a comma-separated image URI list into its non-blank entries.
 */
internal fun splitImageUris(imageUris: String?): List<String> =
    imageUris
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()

/**
 * Reduces a file name to its last path segment so it is safe to use as a
 * storage file name.
 */
internal fun sanitizeFileName(fileName: String): String {
    val sanitized = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
    return sanitized.ifBlank { "attachment" }
}

/**
 * Creates an app-owned file name that retains the supplied display name and
 * extension while preventing same-name attachments from overwriting one
 * another.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun appOwnedStorageFileName(fileName: String): String {
    val displayName = sanitizeFileName(fileName)
    val extensionStart = displayName.lastIndexOf('.').takeIf { it > 0 } ?: displayName.length
    val baseName = displayName.substring(0, extensionStart)
    val extension = displayName.substring(extensionStart)
    return "$baseName-${Uuid.random()}$extension"
}
