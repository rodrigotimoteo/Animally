package com.github.rodrigotimoteo.animally.data.storage

import com.github.rodrigotimoteo.animally.di.infra.appContext
import java.io.File

actual object FileStorage {
    actual fun saveBytes(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val attachmentsDir = File(appContext.filesDir, ATTACHMENTS_DIR).apply { mkdirs() }
        val file = File(attachmentsDir, appOwnedStorageFileName(fileName))
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun delete(path: String): Boolean {
        val target = File(path).canonicalFile
        val allowedDirectories =
            listOf(
                File(appContext.filesDir, ATTACHMENTS_DIR).canonicalFile,
                File(appContext.filesDir, DICTATIONS_DIR).canonicalFile,
            )
        if (allowedDirectories.none { target.parentFile == it }) return false
        return target.delete()
    }

    private const val ATTACHMENTS_DIR = "attachments"
    private const val DICTATIONS_DIR = "dictations"
}
