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

/**
 * Shares a file that already exists on disk (e.g. a written backup) through
 * the platform share sheet.
 *
 * @param fileName the file name shown to the user, including its extension.
 * @param path absolute path of the existing file to share.
 * @param contentType the MIME type of the file, e.g. `application/json`.
 */
expect fun shareFileAt(
    fileName: String,
    path: String,
    contentType: String,
)

/**
 * Shares a PDF document through the platform share sheet.
 *
 * @param fileName the file name shown to the user, ending in `.pdf`.
 * @param bytes the encoded PDF document.
 */
expect fun sharePdf(
    fileName: String,
    bytes: ByteArray,
)
