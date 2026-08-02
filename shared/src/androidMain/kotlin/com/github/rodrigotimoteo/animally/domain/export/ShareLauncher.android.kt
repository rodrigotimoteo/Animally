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
    val file = File(appContext.cacheDir, fileName)
    file.writeText(content)
    shareFileAt(fileName, file.absolutePath, contentType)
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
    val file = File(path)
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = contentType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
}

/**
 * Android implementation: writes [bytes] to the app cache directory and shares
 * it as an `application/pdf` document.
 */
actual fun sharePdf(
    fileName: String,
    bytes: ByteArray,
) {
    val file = File(appContext.cacheDir, fileName)
    file.writeBytes(bytes)
    shareFileAt(fileName, file.absolutePath, "application/pdf")
}
