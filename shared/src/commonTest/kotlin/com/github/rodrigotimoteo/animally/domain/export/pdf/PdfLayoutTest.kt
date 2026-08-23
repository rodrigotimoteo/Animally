package com.github.rodrigotimoteo.animally.domain.export.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the platform-neutral PDF layout engine: long cell values must wrap
 * into multiple text ops with zero content loss; ellipsis is reserved for
 * single unbreakable tokens wider than their whole column.
 */
class PdfLayoutTest {
    @Test
    fun `long cell value wraps into multiple text ops without ellipsis`() {
        val longValue = (1..40).joinToString(separator = " ") { "word$it" }
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Consultation", listOf(listOf("Note"), listOf(longValue))),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()
        val wrappedLines = textOps.filter { it.text.startsWith("word") }

        assertTrue(wrappedLines.size >= 3, "expected >=3 wrapped lines, got ${wrappedLines.size}")
        assertTrue(textOps.none { ELLIPSIS in it.text }, "no cell should be ellipsized")

        val distinctYs = wrappedLines.map { it.y }.distinct()
        assertEquals(wrappedLines.size, distinctYs.size, "wrapped lines must stack on distinct baselines")
    }

    @Test
    fun `wrapped row grows its tint and separator to full height`() {
        val longValue = (1..40).joinToString(separator = " ") { "word$it" }
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Consultation", listOf(listOf("Note"), listOf("short"), listOf(longValue))),
                    ),
            )

        val rects = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Rect>()
        val tintedRowHeights =
            rects
                .filter { it.color == PdfTheme.COLOR_ROW_ALT }
                .map { it.height }

        assertTrue(tintedRowHeights.any { it > PdfTheme.CELL_LINE_HEIGHT + PdfTheme.CELL_ROW_PAD_V })
    }

    @Test
    fun `unbreakable token wider than its column is ellipsized as last resort`() {
        val dominatingValue = "f".repeat(120)
        val unbreakableToken = "u".repeat(80)
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection(
                            "Lab",
                            listOf(listOf("A", "B"), listOf(dominatingValue, unbreakableToken)),
                        ),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()

        assertTrue(textOps.any { it.text.endsWith(ELLIPSIS) }, "oversized single token should be ellipsized")
    }

    @Test
    fun `header cells wrap like body cells`() {
        val longHeader = (1..30).joinToString(separator = " ") { "Header$it" }
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Consultation", listOf(listOf(longHeader), listOf("value"))),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()
        val headerLineOps = textOps.filter { it.bold && it.text.startsWith("Header") && it.text != "Consultation" }

        assertTrue(headerLineOps.size >= 2, "header should wrap onto multiple lines")
        assertFalse(textOps.any { ELLIPSIS in it.text })
    }
}
