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

    @Test
    fun `wide section renders record cards instead of horizontal table`() {
        val headers = listOf("Id", "PatientId", "OvaryStatus", "FollicleSizeMm", "UterineStatus", "Notes")
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Ultrasound", listOf(headers, listOf("1", "1", "Left", "18", "Tight", "ok"))),
                    ),
            )

        val ops = layoutReport(report).flatMap { it.ops }
        val texts = ops.filterIsInstance<PdfOp.Text>()
        val rects = ops.filterIsInstance<PdfOp.Rect>()

        assertTrue(texts.any { it.text == "Record 1" }, "card bar label missing")
        assertTrue(texts.any { it.text == "Ovary Status" }, "labels should use display names")
        val brandRects = rects.filter { it.color == PdfTheme.COLOR_BRAND }
        assertTrue(
            brandRects.all { it.height == PdfTheme.HEADER_BAND_HEIGHT },
            "brand fills must be page bands only — no horizontal table header",
        )
    }

    @Test
    fun `narrow section keeps horizontal table`() {
        val headers = listOf("Id", "PatientId", "Date")
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Consultation", listOf(headers, listOf("7", "1", "2024-06-01"))),
                    ),
            )

        val rects = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Rect>()
        val expectedHeaderHeight = PdfTheme.CELL_LINE_HEIGHT + PdfTheme.HEADER_ROW_PAD_V

        assertTrue(
            rects.any { it.color == PdfTheme.COLOR_BRAND && it.height == expectedHeaderHeight },
            "expected a horizontal table header row",
        )
    }

    @Test
    fun `cards split between pages at card boundaries only`() {
        val headers = listOf("Id", "PatientId", "FieldA", "FieldB", "Notes")
        val rows =
            buildList {
                add(headers)
                (1..16).forEach { index ->
                    add(listOf("$index", "1", "v$index-alpha ${"lorem ".repeat(6)}", "v$index-beta", "v$index-note"))
                }
            }
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections = listOf(PdfSection("Ultrasound", rows)),
            )

        val pages = layoutReport(report)
        assertTrue(pages.size >= 2, "expected the section to span multiple pages")

        (1..16).forEach { record ->
            val cardPage =
                pages.indexOfFirst { page ->
                    page.ops.any { it is PdfOp.Text && it.text == "Record $record" }
                }
            assertTrue(cardPage >= 0, "Record $record missing")
            pages.forEachIndexed { pageIndex, page ->
                page.ops
                    .filterIsInstance<PdfOp.Text>()
                    .filter { it.text.contains("v$record-") }
                    .forEach { _ -> assertEquals(cardPage, pageIndex, "record $record content straddles pages") }
            }
        }
    }

    @Test
    fun `date value appears right-aligned in card header`() {
        val headers = listOf("Id", "PatientId", "Date", "VetName", "Notes")
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Consultation", listOf(headers, listOf("9", "1", "2024-06-01", "Dr. House", "fine"))),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()
        val dateOp = textOps.first { it.text == "2024-06-01" }

        assertTrue(dateOp.rightAligned, "date belongs on the right edge of the card bar")
        assertTrue(dateOp.bold, "date shares the bold card-bar style")
    }

    @Test
    fun `section headers render with csv display names`() {
        val headers = listOf("Id", "TrimOrShoe", "GradeAAEP")
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Lameness", listOf(headers, listOf("3", "Trim", "3"))),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()

        assertTrue(textOps.any { it.text == "Trim/Shoe" }, "TrimOrShoe should render as Trim/Shoe")
        assertTrue(textOps.any { it.text == "AAEP Grade" }, "GradeAAEP should render as AAEP Grade")
        assertFalse(textOps.any { it.text == "TrimOrShoe" })
        assertFalse(textOps.any { it.text == "GradeAAEP" })
    }

    @Test
    fun `card labels render with csv display names`() {
        val headers = listOf("Id", "PatientId", "GradeAAEP", "LimbLocation", "FlexionTest", "Notes")
        val report =
            PdfReportData(
                patient = PdfPatient(name = "Thunder", species = "Equine"),
                sections =
                    listOf(
                        PdfSection("Lameness", listOf(headers, listOf("3", "1", "3", "RF", "Positive", "ok"))),
                    ),
            )

        val textOps = layoutReport(report).flatMap { it.ops }.filterIsInstance<PdfOp.Text>()

        assertTrue(textOps.any { it.text == "AAEP Grade" }, "card labels should use display names")
        assertTrue(textOps.any { it.text == "Flexion Test" }, "card labels should use display names")
    }
}
