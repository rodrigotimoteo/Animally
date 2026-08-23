package com.github.rodrigotimoteo.animally.domain.export.pdf

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// -------------------------------------------------------------------------
// Table preparation
// -------------------------------------------------------------------------

internal fun buildTableBlock(section: PdfSection): TableBlock {
    val headers = section.rows.first()
    val bodyRows = section.rows.drop(1)
    val widths = computeColumnWidths(headers, bodyRows)
    val columnChars = widths.map(::charsPerLine)
    return TableBlock(
        title = section.title,
        widths = widths,
        alignRight = computeNumericAlignment(bodyRows, headers.size),
        headerLines = headers.mapIndexed { index, header -> wrapCell(header, columnChars[index]) },
        bodyRowLines = bodyRows.map { row -> row.mapIndexed { index, cell -> wrapCell(cell, columnChars[index]) } },
    )
}

/**
 * Proportional column widths: each column's weight is its longest content
 * length (header included), normalized so the columns fill the available
 * width exactly. No clamping — long cells simply wrap instead of shrinking
 * other columns or losing text.
 */
internal fun computeColumnWidths(
    headers: List<String>,
    rows: List<List<String>>,
): List<Double> {
    val weights =
        headers.indices.map { index ->
            val longest =
                maxOf(headers[index].length, rows.maxOfOrNull { it.getOrNull(index)?.length ?: 0 } ?: 0)
            longest.toDouble() + PdfTheme.WIDTH_WEIGHT_PADDING
        }
    val totalWeight = weights.sum()
    return weights.map { PdfTheme.CONTENT_WIDTH * it / totalWeight }
}

/** Usable characters per line for a cell of [width] points at the cell font size. */
internal fun charsPerLine(width: Double): Int =
    ((width - 2 * PdfTheme.CELL_PAD_H) / (PdfTheme.CELL_SIZE * PdfTheme.CHAR_WIDTH_FACTOR))
        .toInt()
        .coerceAtLeast(1)

/** Right-aligns a column only when every populated body cell is numeric. */
internal fun computeNumericAlignment(
    rows: List<List<String>>,
    columnCount: Int,
): List<Boolean> =
    (0 until columnCount).map { index ->
        val cells = rows.mapNotNull { it.getOrNull(index) }.filter { it.isNotBlank() }
        cells.isNotEmpty() && cells.all { NUMERIC_CELL.matches(it) }
    }

internal const val ELLIPSIS = "…"

/** Truncates [text] to [maxChars] characters with an ellipsis. Last resort only. */
internal fun ellipsizeToChars(
    text: String,
    maxChars: Int,
): String {
    if (text.length <= maxChars) return text
    return if (maxChars <= ELLIPSIS.length) ELLIPSIS else text.take(maxChars - ELLIPSIS.length) + ELLIPSIS
}

/**
 * Greedy word wrap for a table cell at [maxChars] characters per line.
 *
 * Everything wraps and renders in full; the ONLY truncation is a single
 * unbreakable token (no spaces) that is wider than the entire column — that
 * token is ellipsized, per the layout contract.
 */
internal fun wrapCell(
    text: String,
    maxChars: Int,
): List<String> {
    if (text.isBlank()) return listOf("")
    val words =
        text.split(' ').map { word ->
            if (word.length > maxChars) ellipsizeToChars(word, maxChars) else word
        }
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (candidate.length > maxChars && current.isNotEmpty()) {
            lines += current
            current = word
        } else {
            current = candidate
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

/** Greedy word wrap used for long free-text values such as patient notes. */
internal fun wrapText(
    text: String,
    maxChars: Int,
): List<String> {
    val lines = mutableListOf<String>()
    var current = ""
    text.split(' ').forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (candidate.length > maxChars && current.isNotEmpty()) {
            lines += current
            current = word
        } else {
            current = candidate
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines.ifEmpty { listOf("") }
}

internal val NUMERIC_CELL = Regex("^-?\\d+([.,]\\d+)?$")

// -------------------------------------------------------------------------
// Demographics block
// -------------------------------------------------------------------------

internal fun demographicsLines(report: PdfReportData): List<KeyValueLine> {
    val patient = report.patient
    val lines =
        listOf(
            KeyValueLine("Patient", patient.name),
            patient.species.takeIf { it.isNotBlank() }?.let { KeyValueLine("Species", it) },
            patient.breed?.takeIf { it.isNotBlank() }?.let { KeyValueLine("Breed", it) },
            patient.gender?.takeIf { it.isNotBlank() }?.let { KeyValueLine("Gender", it) },
            patient.dateOfBirth?.let { KeyValueLine("Date of Birth", it.toString()) },
            patient.microchipId?.takeIf { it.isNotBlank() }?.let { KeyValueLine("Microchip", it) },
            patient.ueln?.takeIf { it.isNotBlank() }?.let { KeyValueLine("UELN", it) },
            patient.registrationNumber?.takeIf { it.isNotBlank() }?.let { KeyValueLine("Registration", it) },
            patient.stableLocation?.takeIf { it.isNotBlank() }?.let { KeyValueLine("Stable", it) },
        ).filterNotNull().toMutableList()

    patient.notes?.takeIf { it.isNotBlank() }?.let { notes ->
        val maxChars = (PdfTheme.CONTENT_WIDTH / (PdfTheme.DEMOGRAPHICS_SIZE * PdfTheme.CHAR_WIDTH_FACTOR)).toInt()
        wrapText(notes, maxChars).forEachIndexed { index, wrapped ->
            lines += if (index == 0) KeyValueLine("Notes", wrapped) else KeyValueLine("", wrapped)
        }
    }
    return lines
}

// -------------------------------------------------------------------------
// Timestamp
// -------------------------------------------------------------------------

/** Formats the generation time as `yyyy-MM-dd HH:mm` in the local zone. */
internal fun generationStamp(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    fun pad(value: Int) = value.toString().padStart(2, '0')
    return "${now.year}-${pad(now.monthNumber)}-${pad(now.dayOfMonth)} ${pad(now.hour)}:${pad(now.minute)}"
}
