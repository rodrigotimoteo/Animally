package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * Renders [report] into a PDF document and returns its raw bytes.
 *
 * Platform-specific per ADR-0013: Android renders with the framework
 * `android.graphics.pdf.PdfDocument`; iOS ships a stub for the POC.
 *
 * @param report the report payload to render.
 * @return the encoded PDF bytes, or an empty array on platforms without a
 * renderer yet.
 */
expect fun generatePdf(report: PdfReportData): ByteArray
