package com.github.rodrigotimoteo.animally.domain.export.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val LINE_SPACING = 18f

/**
 * Android implementation: renders the report with the framework
 * [PdfDocument] (zero dependencies) as plain text lines on an A4 page.
 */
actual fun generatePdf(report: PdfReportData): ByteArray {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    var page = document.startPage(pageInfo)
    val titlePaint = paint(textSize = 18f, bold = true)
    val headerPaint = paint(textSize = 11f, bold = true)
    val bodyPaint = paint(textSize = 11f, bold = false)
    var y = MARGIN

    fun drawLine(
        text: String,
        style: Paint,
    ) {
        if (y + LINE_SPACING > PAGE_HEIGHT - MARGIN) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            y = MARGIN
        }
        y = drawWrapped(page.canvas, text, style, y, maxWidth = PAGE_WIDTH - 2 * MARGIN)
    }

    drawLine("Patient History Report", titlePaint)
    drawLine("Patient: ${report.patient.name}", headerPaint)
    patientInfoLines(report).forEach { drawLine(it, bodyPaint) }
    reportDateRange(report)?.let { drawLine(it, bodyPaint) }
    drawLine("", bodyPaint)

    report.sections.forEach { section ->
        drawLine(section.title, titlePaint)
        section.rows.forEachIndexed { index, row ->
            drawLine(row.joinToString(separator = "  |  "), if (index == 0) headerPaint else bodyPaint)
        }
        drawLine("", bodyPaint)
    }

    document.finishPage(page)
    val output = ByteArrayOutputStream()
    document.writeTo(output)
    document.close()
    return output.toByteArray()
}

private fun patientInfoLines(report: PdfReportData): List<String> {
    val patient = report.patient
    return listOfNotNull(
        patient.species.takeIf { it.isNotBlank() }?.let { "Species: $it" },
        patient.breed?.let { "Breed: $it" },
        patient.dateOfBirth?.let { "Date of Birth: $it" },
        patient.gender?.let { "Gender: $it" },
        patient.microchipId?.let { "Microchip: $it" },
        patient.ueln?.let { "UELN: $it" },
        patient.registrationNumber?.let { "Registration: $it" },
        patient.stableLocation?.let { "Stable: $it" },
        patient.notes?.let { "Notes: $it" },
    )
}

private fun reportDateRange(report: PdfReportData): String? {
    val from = report.fromDate
    val to = report.toDate
    return when {
        from != null && to != null -> "Period: $from to $to"
        from != null -> "Period: from $from"
        to != null -> "Period: until $to"
        else -> null
    }
}

private fun paint(
    textSize: Float,
    bold: Boolean,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        this.textSize = textSize
        isFakeBoldText = bold
    }

private fun drawWrapped(
    canvas: Canvas?,
    text: String,
    style: Paint,
    y: Float,
    maxWidth: Float,
): Float {
    if (canvas == null) return y + LINE_SPACING
    var line = ""
    var cursor = y
    text.split(' ').forEach { word ->
        val candidate = if (line.isEmpty()) word else "$line $word"
        if (line.isNotEmpty() && style.measureText(candidate) > maxWidth) {
            canvas.drawText(line, MARGIN, cursor, style)
            cursor += LINE_SPACING
            line = word
        } else {
            line = candidate
        }
    }
    if (line.isNotEmpty()) {
        canvas.drawText(line, MARGIN, cursor, style)
        cursor += LINE_SPACING
    }
    return cursor
}
