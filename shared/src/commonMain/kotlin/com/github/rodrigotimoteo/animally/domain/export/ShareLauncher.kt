package com.github.rodrigotimoteo.animally.domain.export

/**
 * Shares a generated file (e.g. a CSV export) through the platform share sheet.
 *
 * @param fileName the file name shown to the user, including its extension.
 * @param content the file content to share.
 * @param contentType the MIME type of the content, e.g. `text/csv`.
 */
expect fun shareFile(
    fileName: String,
    content: String,
    contentType: String,
)
