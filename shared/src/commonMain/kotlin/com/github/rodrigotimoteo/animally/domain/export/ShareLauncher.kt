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

/**
 * Keeps user-controlled names as a single safe filename component for every
 * platform share implementation.
 */
internal fun sanitizeShareFileName(fileName: String): String {
    val leafName = fileName.substringAfterLast('/').substringAfterLast('\\')
    val sanitized =
        buildString(leafName.length) {
            leafName.forEach { character ->
                when {
                    character.code < FIRST_PRINTABLE_ASCII_CODE || character.code == DELETE_ASCII_CODE -> append('_')
                    character in INVALID_FILENAME_CHARACTERS -> append('_')
                    else -> append(character)
                }
            }
        }.trim()
    return sanitized.takeUnless { it.isEmpty() || it == "." || it == ".." } ?: DEFAULT_SHARE_FILE_NAME
}

private val INVALID_FILENAME_CHARACTERS = setOf('<', '>', ':', '"', '|', '?', '*')
private const val DEFAULT_SHARE_FILE_NAME = "export"
private const val FIRST_PRINTABLE_ASCII_CODE = 0x20
private const val DELETE_ASCII_CODE = 0x7F
