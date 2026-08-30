package com.github.rodrigotimoteo.animally.domain.export.pdf

internal fun paginate(report: PdfReportData): List<List<PdfOp>> {
    val pages = mutableListOf<MutableList<PdfOp>>()
    var ops = newContentPage(pages)
    var y = PdfTheme.CONTENT_TOP

    for (line in demographicsLines(report)) {
        if (y + PdfTheme.DEMOGRAPHICS_LINE_HEIGHT > PdfTheme.CONTENT_BOTTOM) {
            ops = newContentPage(pages)
            y = PdfTheme.CONTENT_TOP
        }
        ops += keyValueOps(line, y)
        y += PdfTheme.DEMOGRAPHICS_LINE_HEIGHT
    }

    val tables = report.sections.filter { it.rows.size > 1 }.map(::buildSectionBlock)
    if (tables.isEmpty()) {
        ops += emptyNoticeOp(y)
        return pages
    }

    for (block in tables) {
        y =
            when (block) {
                is TableBlock -> drawTable(block, pages, y)
                is RecordCardBlock -> drawCards(block, pages, y)
            }
    }
    return pages
}

internal fun newContentPage(pages: MutableList<MutableList<PdfOp>>): MutableList<PdfOp> {
    val ops = mutableListOf<PdfOp>()
    pages += ops
    return ops
}

private fun keyValueOps(
    line: KeyValueLine,
    y: Double,
): List<PdfOp> =
    listOf(
        PdfOp.Text(
            line.label,
            PdfTheme.MARGIN + PdfTheme.DEMO_LABEL_RIGHT_EDGE_OFFSET,
            y,
            PdfTheme.DEMOGRAPHICS_SIZE,
            true,
            PdfTheme.COLOR_LABEL,
            rightAligned = true,
        ),
        PdfOp.Text(
            line.value,
            PdfTheme.MARGIN + PdfTheme.DEMO_VALUE_X_OFFSET,
            y,
            PdfTheme.DEMOGRAPHICS_SIZE,
            false,
            PdfTheme.COLOR_TEXT,
        ),
    )

private fun emptyNoticeOp(y: Double): PdfOp =
    PdfOp.Text(
        "No records in range",
        PdfTheme.PAGE_WIDTH / 2,
        y + PdfTheme.EMPTY_NOTICE_OFFSET_Y,
        PdfTheme.SECTION_TITLE_SIZE,
        false,
        PdfTheme.COLOR_MUTED,
        centered = true,
    )
