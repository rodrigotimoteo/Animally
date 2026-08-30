package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * A section table prepared for drawing: column widths, alignment and every
 * cell already wrapped into its final lines. Row heights are dynamic — the
 * tallest cell's line count drives them — so no content is ever truncated.
 */
internal class TableBlock(
    override val title: String,
    val widths: List<Double>,
    val alignRight: List<Boolean>,
    val headerLines: List<List<String>>,
    val bodyRowLines: List<List<List<String>>>,
) : SectionBlock {
    /** Height of a row whose cells wrap to [cells] lines. */
    fun rowHeight(cells: List<List<String>>): Double =
        cells
            .maxOf { it.size } * PdfTheme.CELL_LINE_HEIGHT + PdfTheme.CELL_ROW_PAD_V

    /** Height of the wrapped header row (uses the header-specific padding). */
    fun headerHeight(): Double = headerLines.maxOf { it.size } * PdfTheme.CELL_LINE_HEIGHT + PdfTheme.HEADER_ROW_PAD_V

    /**
     * Draws one row of wrapped cells at [y]. Each cell's lines are stacked
     * vertically and centered within the row height; right-aligned columns
     * align each line's right edge.
     */
    fun textOps(
        cells: List<List<String>>,
        y: Double,
        bold: Boolean,
        color: Long,
    ): List<PdfOp> {
        val rowHeight = rowHeight(cells)
        return cells.flatMapIndexed { index, lines ->
            val cellX = PdfTheme.MARGIN + widths.take(index).sum()
            val rightAligned = alignRight.getOrElse(index) { false }
            val textX = if (rightAligned) cellX + widths[index] - PdfTheme.CELL_PAD_H else cellX + PdfTheme.CELL_PAD_H
            val blockHeight = lines.size * PdfTheme.CELL_LINE_HEIGHT
            val topOffset = (rowHeight - blockHeight) / 2
            lines.mapIndexed { lineIndex, line ->
                PdfOp.Text(
                    line,
                    textX,
                    y + topOffset + lineIndex * PdfTheme.CELL_LINE_HEIGHT,
                    PdfTheme.CELL_SIZE,
                    bold,
                    color,
                    rightAligned = rightAligned,
                )
            }
        }
    }
}

/** One record of a [RecordCardBlock]: optional date for the bar plus label/value pairs. */
internal class CardRecord(
    val date: String?,
    val pairs: List<CardPair>,
)

/** One label/value pair of a record card, both sides already wrapped into lines. */
internal class CardPair(
    val labelLines: List<String>,
    val valueLines: List<String>,
) {
    /** Pair height driven by the taller side. */
    fun height(): Double {
        val lineCount = maxOf(labelLines.size, valueLines.size)
        return lineCount * PdfTheme.CELL_LINE_HEIGHT + PdfTheme.CARD_PAIR_PAD_V
    }

    /** Draws the pair at [y], each side vertically centered within [pairHeight]. */
    fun textOps(
        labelX: Double,
        valueX: Double,
        y: Double,
        pairHeight: Double,
    ): List<PdfOp> {
        val blockHeight = maxOf(labelLines.size, valueLines.size) * PdfTheme.CELL_LINE_HEIGHT
        val topOffset = (pairHeight - blockHeight) / 2
        val labelOps =
            labelLines.mapIndexed { index, line ->
                PdfOp.Text(
                    line,
                    labelX,
                    y + topOffset + index * PdfTheme.CELL_LINE_HEIGHT,
                    PdfTheme.CELL_SIZE,
                    true,
                    PdfTheme.COLOR_LABEL,
                )
            }
        val valueOps =
            valueLines.mapIndexed { index, line ->
                PdfOp.Text(
                    line,
                    valueX,
                    y + topOffset + index * PdfTheme.CELL_LINE_HEIGHT,
                    PdfTheme.CELL_SIZE,
                    false,
                    PdfTheme.COLOR_TEXT,
                )
            }
        return labelOps + valueOps
    }
}

/**
 * A wide section (>4 columns) rendered as stacked vertical record cards:
 * tinted bar with "Record N" + date, then label/value pairs. Cards split
 * only between records — never mid-card.
 */
internal class RecordCardBlock(
    override val title: String,
    val records: List<CardRecord>,
) : SectionBlock {
    private val labelWidth = PdfTheme.CONTENT_WIDTH * PdfTheme.CARD_LABEL_WIDTH_FRACTION
    private val labelX = PdfTheme.MARGIN + PdfTheme.CARD_PAD_H
    private val valueX = PdfTheme.MARGIN + labelWidth

    /** Height of one card: header bar plus every pair's height. */
    fun recordHeight(record: CardRecord): Double = PdfTheme.CARD_HEADER_HEIGHT + record.pairs.sumOf { it.height() }

    /** Draws one card at [y]. */
    fun cardOps(
        record: CardRecord,
        index: Int,
        y: Double,
    ): List<PdfOp> =
        buildList {
            add(
                PdfOp.Rect(
                    PdfTheme.MARGIN,
                    y,
                    PdfTheme.CONTENT_WIDTH,
                    PdfTheme.CARD_HEADER_HEIGHT,
                    PdfTheme.COLOR_CARD_TINT,
                ),
            )
            val headerTextY = y + PdfTheme.CARD_HEADER_TEXT_OFFSET_Y
            add(
                PdfOp.Text(
                    "Record ${index + 1}",
                    labelX,
                    headerTextY,
                    PdfTheme.CARD_TEXT_SIZE,
                    true,
                    PdfTheme.COLOR_TEXT,
                ),
            )
            record.date?.let { date ->
                add(
                    PdfOp.Text(
                        date,
                        PdfTheme.MARGIN + PdfTheme.CONTENT_WIDTH - PdfTheme.CARD_PAD_H,
                        headerTextY,
                        PdfTheme.CARD_TEXT_SIZE,
                        true,
                        PdfTheme.COLOR_TEXT,
                        rightAligned = true,
                    ),
                )
            }
            var pairY = y + PdfTheme.CARD_HEADER_HEIGHT
            record.pairs.forEachIndexed { pairIndex, pair ->
                val pairHeight = pair.height()
                addAll(pair.textOps(labelX, valueX, pairY, pairHeight))
                if (pairIndex < record.pairs.lastIndex) {
                    add(
                        PdfOp.Rect(
                            PdfTheme.MARGIN,
                            pairY + pairHeight - PdfTheme.SEPARATOR_THICKNESS,
                            PdfTheme.CONTENT_WIDTH,
                            PdfTheme.SEPARATOR_THICKNESS,
                            PdfTheme.COLOR_SEPARATOR,
                        ),
                    )
                }
                pairY += pairHeight
            }
        }
}

// -------------------------------------------------------------------------
// Section drawing helpers — paginator delegates here
// -------------------------------------------------------------------------

/**
 * Draws [table] starting at [y], splitting across pages at row boundaries.
 * Row heights are dynamic (wrapped cells grow). The header row is never
 * orphaned: when it would not fit together with at least one body row, the
 * whole table moves to the next page. Continuation pages repeat the section
 * title and header row. Returns the new y cursor.
 */
internal fun drawTable(
    table: TableBlock,
    pages: MutableList<MutableList<PdfOp>>,
    startY: Double,
): Double {
    var ops = pages.last()
    var y = startY
    val headBlockHeight = PdfTheme.SECTION_TITLE_HEIGHT + PdfTheme.TITLE_TO_TABLE_GAP + table.headerHeight()

    fun startNewPage() {
        ops = newContentPage(pages)
        y = PdfTheme.CONTENT_TOP
        ops += tableHeadOps(table, y)
        y += headBlockHeight
    }

    val firstRowHeight = table.bodyRowLines.firstOrNull()?.let(table::rowHeight) ?: 0.0
    if (y + headBlockHeight + firstRowHeight > PdfTheme.CONTENT_BOTTOM) {
        ops = newContentPage(pages)
        y = PdfTheme.CONTENT_TOP
    }
    ops += tableHeadOps(table, y)
    y += headBlockHeight

    table.bodyRowLines.forEachIndexed { index, cells ->
        val rowHeight = table.rowHeight(cells)
        if (y + rowHeight > PdfTheme.CONTENT_BOTTOM) {
            startNewPage()
        }
        ops += bodyRowOps(table, cells, y, zebra = index % 2 == 1)
        y += rowHeight
    }
    return y + PdfTheme.SECTION_GAP
}

private fun tableHeadOps(
    table: TableBlock,
    y: Double,
): List<PdfOp> =
    buildList {
        add(
            PdfOp.Text(table.title, PdfTheme.MARGIN, y, PdfTheme.SECTION_TITLE_SIZE, true, PdfTheme.COLOR_TEXT),
        )
        val headerY = y + PdfTheme.SECTION_TITLE_HEIGHT + PdfTheme.TITLE_TO_TABLE_GAP
        add(
            PdfOp.Rect(
                PdfTheme.MARGIN,
                headerY,
                PdfTheme.CONTENT_WIDTH,
                table.headerHeight(),
                PdfTheme.COLOR_BRAND,
            ),
        )
        addAll(table.textOps(table.headerLines, headerY, bold = true, color = PdfTheme.COLOR_WHITE))
    }

private fun bodyRowOps(
    table: TableBlock,
    cells: List<List<String>>,
    y: Double,
    zebra: Boolean,
): List<PdfOp> {
    val rowHeight = table.rowHeight(cells)
    return buildList {
        if (zebra) {
            add(PdfOp.Rect(PdfTheme.MARGIN, y, PdfTheme.CONTENT_WIDTH, rowHeight, PdfTheme.COLOR_ROW_ALT))
        }
        add(
            PdfOp.Rect(
                PdfTheme.MARGIN,
                y + rowHeight - PdfTheme.SEPARATOR_THICKNESS,
                PdfTheme.CONTENT_WIDTH,
                PdfTheme.SEPARATOR_THICKNESS,
                PdfTheme.COLOR_SEPARATOR,
            ),
        )
        addAll(table.textOps(cells, y, bold = false, color = PdfTheme.COLOR_TEXT))
    }
}

/**
 * Draws [block]'s cards starting at [y], splitting between cards only.
 * Continuation pages repeat the section title (not any table header).
 */
internal fun drawCards(
    block: RecordCardBlock,
    pages: MutableList<MutableList<PdfOp>>,
    startY: Double,
): Double {
    var ops = pages.last()
    var y = startY
    val titleHeight = PdfTheme.SECTION_TITLE_HEIGHT + PdfTheme.TITLE_TO_TABLE_GAP

    fun drawTitle() {
        ops += PdfOp.Text(block.title, PdfTheme.MARGIN, y, PdfTheme.SECTION_TITLE_SIZE, true, PdfTheme.COLOR_TEXT)
        y += titleHeight
    }

    fun startNewPage() {
        ops = newContentPage(pages)
        y = PdfTheme.CONTENT_TOP
        drawTitle()
    }

    val firstCardHeight = block.records.firstOrNull()?.let(block::recordHeight) ?: 0.0
    if (y + titleHeight + firstCardHeight > PdfTheme.CONTENT_BOTTOM) {
        startNewPage()
    } else {
        drawTitle()
    }

    block.records.forEachIndexed { index, record ->
        val cardHeight = block.recordHeight(record)
        if (y + cardHeight > PdfTheme.CONTENT_BOTTOM) {
            startNewPage()
        }
        ops += block.cardOps(record, index, y)
        y += cardHeight
        if (index < block.records.lastIndex) {
            y += PdfTheme.CARD_GAP
        }
    }
    return y + PdfTheme.SECTION_GAP
}
