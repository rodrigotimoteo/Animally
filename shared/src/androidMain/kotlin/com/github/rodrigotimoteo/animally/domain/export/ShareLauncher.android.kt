package com.github.rodrigotimoteo.animally.domain.export

import android.content.Intent
import androidx.core.content.FileProvider
import com.github.rodrigotimoteo.animally.di.infra.appContext
import java.io.File

/**
 * Android implementation: writes [content] to the app cache directory and
 * launches the system share sheet via `ACTION_SEND` with a content URI.
 */
actual fun shareFile(
    fileName: String,
    content: String,
    contentType: String,
) {
    val safeFileName = sanitizeShareFileName(fileName)
    val file = File(appContext.cacheDir, safeFileName)
    file.writeText(content)
    shareFileAt(safeFileName, file.absolutePath, contentType)
}

/**
 * Android implementation: shares the existing file at [path] through the
 * system share sheet via `ACTION_SEND` with a content URI.
 */
actual fun shareFileAt(
    fileName: String,
    path: String,
    contentType: String,
) {
    val context = appContext
    val safeFileName = sanitizeShareFileName(fileName)
    val file = requireShareableFile(context, path)
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = contentType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(Intent.createChooser(shareIntent, "Share $safeFileName"))
}

private fun requireShareableFile(
    context: android.content.Context,
    path: String,
): File {
    val file =
        runCatching { File(path).canonicalFile }
            .getOrElse { error("Cannot resolve file for sharing") }
    check(file.isFile) { "Only an existing regular file may be shared" }
    val allowedRoots =
        listOf(
            context.cacheDir,
            File(context.filesDir, "backups"),
        ).map { it.canonicalFile }
    check(allowedRoots.any { root -> file.path.startsWith(root.path + File.separator) }) {
        "File is outside Animally's shareable storage"
    }
    return file
}

/**
 * Android implementation: writes [bytes] to the app cache directory and shares
 * it as an `application/pdf` document.
 */
actual fun sharePdf(
    fileName: String,
    bytes: ByteArray,
) {
    val safeFileName = sanitizeShareFileName(fileName)
    val file = File(appContext.cacheDir, safeFileName)
    file.writeBytes(bytes)
    shareFileAt(safeFileName, file.absolutePath, "application/pdf")
}
