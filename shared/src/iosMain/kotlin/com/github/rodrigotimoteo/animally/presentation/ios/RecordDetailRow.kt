@file:OptIn(ExperimentalObjCName::class, ExperimentalForeignApi::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.snprintf
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/** Buffer size for the one-decimal printf output; ample for any double. */
private const val FORMAT_BUFFER_SIZE: UInt = 32u

/**
 * One ready-to-render label/value pair of the read-only record detail.
 *
 * Built on the Kotlin side from the loaded edit-form state so the Swift view
 * never re-derives per-type content from strings.
 */
@ObjCName("RecordDetailRow")
data class RecordDetailRow(
    val label: String,
    val value: String,
)

/**
 * Maps label/value pairs to [RecordDetailRow]s, dropping pairs whose value is
 * blank — the same filtering the tab views apply to their preview rows.
 */
internal fun recordDetailRows(pairs: List<Pair<String, String?>>): List<RecordDetailRow> =
    pairs
        .map { RecordDetailRow(label = it.first, value = it.second ?: "") }
        .filter { it.value.isNotEmpty() }

/**
 * Formats a double with exactly one decimal digit, producing byte-identical
 * output to Swift's `String(format: "%.1f", …)` by going through the same
 * libc printf.
 */
internal fun formatOneDecimal(value: Double): String =
    memScoped {
        val buffer = allocArray<ByteVar>(FORMAT_BUFFER_SIZE.toInt())
        snprintf(buffer, FORMAT_BUFFER_SIZE.toULong(), "%.1f", value)
        buffer.toKString()
    }
