package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * A single drawing instruction of the platform-neutral PDF layout model.
 *
 * Rectangles use a top-left origin; text [PdfOp.Text.y] is the top of the
 * text box. Platform painters translate these to their native APIs.
 */
internal sealed interface PdfOp {
    /**
     * Fills a rectangle.
     *
     * @property color ARGB fill color from [PdfTheme].
     */
    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val color: Long,
    ) : PdfOp

    /**
     * Draws one line of text.
     *
     * @property y top edge of the text box.
     * @property rightAligned when set, [x] is the right edge.
     * @property centered when set, [x] is the center.
     */
    data class Text(
        val text: String,
        val x: Double,
        val y: Double,
        val size: Double,
        val bold: Boolean,
        val color: Long,
        val rightAligned: Boolean = false,
        val centered: Boolean = false,
    ) : PdfOp
}

/** One laid-out page of the report, with its footer metadata resolved. */
internal data class PdfPageModel(
    val pageNumber: Int,
    val totalPages: Int,
    val ops: List<PdfOp>,
)

/** One atomic line of the demographics grid. */
internal data class KeyValueLine(
    val label: String,
    val value: String,
)

/** A section laid out either as a horizontal table or as vertical record cards. */
internal sealed interface SectionBlock {
    val title: String
}
