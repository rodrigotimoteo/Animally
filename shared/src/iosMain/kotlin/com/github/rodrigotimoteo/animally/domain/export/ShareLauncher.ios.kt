@file:OptIn(ExperimentalObjCName::class, ExperimentalForeignApi::class)
@file:Suppress("MatchingDeclarationName")

package com.github.rodrigotimoteo.animally.domain.export

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing hook that receives the absolute path of every exported
 * artifact so the native layer can present the system share sheet.
 *
 * Kotlin cannot present `UIActivityViewController` itself, so the iOS app
 * installs a listener here once at startup (`ExportShareInstaller.install()`).
 */
@ObjCName("ExportShareHook")
object ExportShareHook {
    /** Invoked with the path of the file to share; may be called from any thread. */
    var onShareRequest: ((path: String) -> Unit)? = null
}

/**
 * iOS implementation: writes [content] into `Documents/exports/[fileName]`
 * and hands the path to [ExportShareHook] for share-sheet presentation.
 */
actual fun shareFile(
    fileName: String,
    content: String,
    contentType: String,
) {
    val targetPath = "${ensureExportsDir()}/$fileName"
    val written =
        NSString
            .create(string = content)
            .writeToFile(path = targetPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    check(written) { "Failed to write export file to $targetPath" }
    ExportShareHook.onShareRequest?.invoke(targetPath)
}

/**
 * iOS implementation: copies the existing file at [path] into
 * `Documents/exports/[fileName]` and hands the copy's path to
 * [ExportShareHook].
 */
actual fun shareFileAt(
    fileName: String,
    path: String,
    contentType: String,
) {
    val fileManager = NSFileManager.defaultManager
    check(fileManager.fileExistsAtPath(path)) { "Cannot share missing file at $path" }
    val targetPath = "${ensureExportsDir()}/$fileName"
    if (fileManager.fileExistsAtPath(targetPath)) {
        fileManager.removeItemAtPath(targetPath, error = null)
    }
    val copied = fileManager.copyItemAtPath(path, toPath = targetPath, error = null)
    check(copied) { "Failed to copy export file from $path to $targetPath" }
    ExportShareHook.onShareRequest?.invoke(targetPath)
}

/**
 * iOS implementation: writes [bytes] into `Documents/exports/[fileName]` and
 * hands the path to [ExportShareHook].
 */
actual fun sharePdf(
    fileName: String,
    bytes: ByteArray,
) {
    val targetPath = "${ensureExportsDir()}/$fileName"
    val data =
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    val written = data.writeToFile(targetPath, atomically = true)
    check(written) { "Failed to write export file to $targetPath" }
    ExportShareHook.onShareRequest?.invoke(targetPath)
}

/** Creates `Documents/exports` when missing and returns its absolute path. */
private fun ensureExportsDir(): String {
    val documentsUrl =
        requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            ),
        )
    val documentsPath = requireNotNull(documentsUrl.path) { "Failed to resolve Documents directory" }
    val exportsDirPath = "$documentsPath/$EXPORTS_DIR"
    NSFileManager.defaultManager.createDirectoryAtPath(
        exportsDirPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return exportsDirPath
}

private const val EXPORTS_DIR = "exports"
