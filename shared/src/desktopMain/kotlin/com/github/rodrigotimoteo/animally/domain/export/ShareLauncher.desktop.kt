@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.domain.export

/**
 * Desktop stub for the POC — real file-open/share wiring is deferred.
 */
actual fun shareFile(
    fileName: String,
    content: String,
    contentType: String,
) = Unit

/**
 * Desktop stub for the POC — real file-open/share wiring is deferred.
 */
actual fun shareFileAt(
    fileName: String,
    path: String,
    contentType: String,
) = Unit

/**
 * Desktop stub for the POC — real file-open/share wiring is deferred.
 */
actual fun sharePdf(
    fileName: String,
    bytes: ByteArray,
) = Unit
