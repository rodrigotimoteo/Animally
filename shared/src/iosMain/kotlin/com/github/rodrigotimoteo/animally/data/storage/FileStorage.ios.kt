package com.github.rodrigotimoteo.animally.data.storage

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

actual object FileStorage {
    actual fun saveBytes(
        fileName: String,
        bytes: ByteArray,
    ): String {
        val fileManager = NSFileManager.defaultManager
        val documentsUrl =
            fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
        val attachmentsUrl =
            requireNotNull(documentsUrl)
                .URLByAppendingPathComponent(ATTACHMENTS_DIR, isDirectory = true)
        fileManager.createDirectoryAtURL(
            attachmentsUrl,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val fileUrl = attachmentsUrl.URLByAppendingPathComponent(sanitizeFileName(fileName), isDirectory = false)
        val data =
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
        data.writeToURL(fileUrl, atomically = true)
        return requireNotNull(fileUrl.path) { "Failed to resolve attachment path" }
    }

    private const val ATTACHMENTS_DIR = "attachments"
}
