@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.data.storage

import java.io.File

/**
 * Desktop implementation: writes into `<tmpdir>/animally/attachments`.
 */
actual object FileStorage {
    actual fun saveBytes(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val attachmentsDir = File(storageRoot(), ATTACHMENTS_DIR).apply { mkdirs() }
        val file = File(attachmentsDir, appOwnedStorageFileName(fileName))
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun delete(path: String): Boolean {
        val target = File(path).canonicalFile
        val allowedDirectories =
            listOf(
                File(storageRoot(), ATTACHMENTS_DIR).canonicalFile,
                File(storageRoot(), DICTATIONS_DIR).canonicalFile,
            )
        if (allowedDirectories.none { target.parentFile == it }) return false
        return target.delete()
    }

    private fun storageRoot(): File = File(System.getProperty("java.io.tmpdir"), "animally")

    private const val ATTACHMENTS_DIR = "attachments"
    private const val DICTATIONS_DIR = "dictations"
}
