package com.github.rodrigotimoteo.animally.data.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
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
            requireNotNull(
                requireNotNull(documentsUrl)
                    .URLByAppendingPathComponent(ATTACHMENTS_DIR, isDirectory = true),
            )
        fileManager.createDirectoryAtURL(
            attachmentsUrl,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val fileUrl =
            requireNotNull(
                attachmentsUrl.URLByAppendingPathComponent(sanitizeFileName(fileName), isDirectory = false),
            )
        val data =
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
        val wrote = data.writeToURL(fileUrl, atomically = true)
        check(wrote) { "Failed to write attachment to $fileUrl" }
        return requireNotNull(fileUrl.path) { "Failed to resolve attachment path" }
    }

    actual fun delete(path: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        val documentsPath = documentsDirectoryPath() ?: return false
        // Standardize to resolve ".." / "." before allow-list check; hardens prefix guard.
        val standardized = standardizePath(path)
        val allowedRoots =
            listOf(
                standardizePath("$documentsPath/$ATTACHMENTS_DIR"),
                standardizePath("$documentsPath/$DICTATIONS_DIR"),
            )
        val insideAllowed =
            allowedRoots.any { root ->
                standardized == root || standardized.startsWith("$root/")
            }
        if (standardized.contains("..") || !insideAllowed) return false
        return fileManager.removeItemAtPath(standardized, error = null)
    }

    private fun standardizePath(path: String): String {
        val isAbsolute = path.startsWith("/")
        val stack = mutableListOf<String>()
        for (segment in path.split('/')) {
            when {
                segment.isEmpty() || segment == "." -> Unit
                segment == ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else stack.add(segment)
                else -> stack.add(segment)
            }
        }
        val joined = stack.joinToString("/")
        return if (isAbsolute) "/$joined" else joined
    }

    private fun documentsDirectoryPath(): String? =
        NSFileManager.defaultManager
            .URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )?.path

    private const val ATTACHMENTS_DIR = "attachments"
    private const val DICTATIONS_DIR = "dictations"
}
