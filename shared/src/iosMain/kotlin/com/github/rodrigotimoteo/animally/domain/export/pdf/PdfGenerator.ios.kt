package com.github.rodrigotimoteo.animally.domain.export.pdf

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.drawAtPoint
import platform.posix.memcpy

private const val PAGE_WIDTH = 612.0
private const val PAGE_HEIGHT = 792.0
private const val MARGIN = 40.0
private const val LINE_SPACING = 18.0
private const val BOTTOM_MARGIN = 50.0

private const val TITLE_SIZE = 18.0
private const val SECTION_SIZE = 14.0
private const val BODY_SIZE = 12.0

/**
 * iOS implementation: renders the report with `UIGraphicsPDFRenderer` as
 * plain text lines on US Letter pages, mirroring the Android layout.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun generatePdf(report: PdfReportData): ByteArray {
    val renderer =
        UIGraphicsPDFRenderer(
            bounds = CGRectMake(0.0, 0.0, PAGE_WIDTH, PAGE_HEIGHT),
            format = UIGraphicsPDFRendererFormat(),
        )
    val titleFont = UIFont.boldSystemFontOfSize(TITLE_SIZE)
    val sectionFont = UIFont.boldSystemFontOfSize(SECTION_SIZE)
    val bodyFont = UIFont.systemFontOfSize(BODY_SIZE)

    var y = MARGIN
    val data =
        renderer.PDFDataWithActions { context ->
            val pdfContext = requireNotNull(context)
            pdfContext.beginPage()

            fun drawLine(
                text: String,
                font: UIFont,
            ) {
                if (y + LINE_SPACING > PAGE_HEIGHT - BOTTOM_MARGIN) {
                    pdfContext.beginPage()
                    y = MARGIN
                }
                (text as NSString).drawAtPoint(
                    point = CGPointMake(MARGIN, y),
                    withAttributes = mapOf(NSFontAttributeName to font),
                )
                y += LINE_SPACING
            }

            drawLine("Patient History Report", titleFont)
            drawLine("Patient: ${report.patient.name}", sectionFont)
            patientInfoLines(report).forEach { drawLine(it, bodyFont) }
            reportDateRange(report)?.let { drawLine(it, bodyFont) }
            drawLine("", bodyFont)

            report.sections.forEach { section ->
                drawLine(section.title, sectionFont)
                section.rows.forEachIndexed { index, row ->
                    drawLine(row.joinToString(separator = "  |  "), if (index == 0) sectionFont else bodyFont)
                }
                drawLine("", bodyFont)
            }
        }
    return data.toByteArray()
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

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return result
}
