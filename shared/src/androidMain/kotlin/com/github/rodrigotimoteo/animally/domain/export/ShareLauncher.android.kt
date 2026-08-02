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
    val context = appContext
    val file = File(context.cacheDir, fileName)
    file.writeText(content)
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = contentType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(shareIntent, "Share $fileName"))
}
