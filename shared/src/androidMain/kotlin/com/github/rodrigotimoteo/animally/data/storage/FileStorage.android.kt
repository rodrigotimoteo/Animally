package com.github.rodrigotimoteo.animally.data.storage

import com.github.rodrigotimoteo.animally.di.infra.appContext
import java.io.File

actual object FileStorage {
    actual fun saveBytes(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val attachmentsDir = File(appContext.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
        val file = File(attachmentsDir, sanitizeFileName(fileName))
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private const val ATTACHMENTS_DIR = "attachments"
}
