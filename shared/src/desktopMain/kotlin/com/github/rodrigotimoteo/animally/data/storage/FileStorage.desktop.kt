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
        val file = File(attachmentsDir, sanitizeFileName(fileName))
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private fun storageRoot(): File = File(System.getProperty("java.io.tmpdir"), "animally")

    private const val ATTACHMENTS_DIR = "attachments"
}
