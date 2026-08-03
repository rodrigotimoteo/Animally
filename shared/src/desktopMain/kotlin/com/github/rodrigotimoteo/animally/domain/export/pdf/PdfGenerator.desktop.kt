@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * Desktop stub for the POC — a PDF renderer is deferred per ADR-0013. Returns
 * an empty payload so callers compile unchanged.
 */
actual fun generatePdf(report: PdfReportData): ByteArray = ByteArray(0)
