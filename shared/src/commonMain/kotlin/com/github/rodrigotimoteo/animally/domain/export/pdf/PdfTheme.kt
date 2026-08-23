package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * Single source of truth for the patient-history PDF look.
 *
 * Both platform renderers (Android `PdfDocument`/Canvas and iOS
 * `UIGraphicsPDFRenderer`) read these constants so the documents are visually
 * identical. Coordinates use a top-left origin with points as the unit, which
 * matches UIKit's flipped coordinate space; the Android painter compensates
 * for text baselines locally.
 */
object PdfTheme {
    // ---------------------------------------------------------------------
    // Page geometry (US Letter)
    // ---------------------------------------------------------------------
    const val PAGE_WIDTH = 612.0
    const val PAGE_HEIGHT = 792.0
    const val MARGIN = 40.0
    const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    // ---------------------------------------------------------------------
    // Colors (ARGB)
    // ---------------------------------------------------------------------
    const val COLOR_BRAND = 0xFF2E5E4EL
    const val COLOR_TEXT = 0xFF1F2937L
    const val COLOR_LABEL = 0xFF5B6660L
    const val COLOR_ROW_ALT = 0xFFF3F4F3L
    const val COLOR_SEPARATOR = 0xFFD9DDDAL
    const val COLOR_FOOTER = 0xFF8A8F8CL
    const val COLOR_MUTED = 0xFF9AA19DL
    const val COLOR_WHITE = 0xFFFFFFFFL

    // ---------------------------------------------------------------------
    // Type sizes (points)
    // ---------------------------------------------------------------------
    const val BRAND_SIZE = 20.0
    const val BAND_TITLE_SIZE = 13.0
    const val BAND_DATE_SIZE = 10.0
    const val SECTION_TITLE_SIZE = 14.0
    const val DEMOGRAPHICS_SIZE = 11.0
    const val CELL_SIZE = 10.0
    const val FOOTER_SIZE = 8.5

    // ---------------------------------------------------------------------
    // Vertical rhythm
    // ---------------------------------------------------------------------
    const val HEADER_BAND_HEIGHT = 72.0
    const val BAND_PAD_H = 14.0
    const val BRAND_TEXT_OFFSET_Y = 12.0
    const val BAND_TITLE_OFFSET_Y = 42.0
    const val BAND_DATE_OFFSET_Y = 16.0
    const val CONTENT_TOP = MARGIN + HEADER_BAND_HEIGHT + 16.0
    const val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN
    const val DEMOGRAPHICS_LINE_HEIGHT = 17.0
    const val SECTION_TITLE_HEIGHT = 20.0
    const val TITLE_TO_TABLE_GAP = 7.0
    const val SECTION_GAP = 24.0
    const val FOOTER_PAGE_LINE_Y = PAGE_HEIGHT - 26.0
    const val FOOTER_STAMP_LINE_Y = PAGE_HEIGHT - 15.0

    // ---------------------------------------------------------------------
    // Wrapped-cell metrics
    // ---------------------------------------------------------------------

    /** Height of one wrapped text line inside a cell (10pt font + leading). */
    const val CELL_LINE_HEIGHT = 12.0

    /** Total vertical padding of a body row; 1-line rows are therefore 17pt tall. */
    const val CELL_ROW_PAD_V = 5.0

    /** Total vertical padding of a header row; 1-line headers are therefore 19pt tall. */
    const val HEADER_ROW_PAD_V = 7.0

    /**
     * Generous guard against pathological pages: cells wrap to at most this
     * many lines (64 ≈ two full pages of text). Effectively unlimited for
     * real veterinary records — no meaningful content is ever dropped.
     */
    const val MAX_CELL_LINES = 64

    // ---------------------------------------------------------------------
    // Table metrics
    // ---------------------------------------------------------------------
    const val CELL_PAD_H = 5.0
    const val SEPARATOR_THICKNESS = 0.5

    // Column sizing
    const val WIDTH_WEIGHT_PADDING = 2.0

    // Demographics grid geometry
    const val DEMO_LABEL_RIGHT_EDGE_OFFSET = 110.0
    const val DEMO_VALUE_X_OFFSET = 122.0
    const val EMPTY_NOTICE_OFFSET_Y = 12.0

    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val CHANNEL_MASK = 0xFFL
    private const val CHANNEL_SCALE = 255.0

    /**
     * Approximate glyph width as a fraction of the font size; used for
     * deterministic column sizing and ellipsis on both platforms.
     */
    const val CHAR_WIDTH_FACTOR = 0.52

    /** Extracts the alpha channel of an ARGB [argb] color as `0.0..1.0`. */
    fun alpha(argb: Long): Double = ((argb shr ALPHA_SHIFT) and CHANNEL_MASK) / CHANNEL_SCALE

    /** Extracts the red channel of an ARGB [argb] color as `0.0..1.0`. */
    fun red(argb: Long): Double = ((argb shr RED_SHIFT) and CHANNEL_MASK) / CHANNEL_SCALE

    /** Extracts the green channel of an ARGB [argb] color as `0.0..1.0`. */
    fun green(argb: Long): Double = ((argb shr GREEN_SHIFT) and CHANNEL_MASK) / CHANNEL_SCALE

    /** Extracts the blue channel of an ARGB [argb] color as `0.0..1.0`. */
    fun blue(argb: Long): Double = (argb and CHANNEL_MASK) / CHANNEL_SCALE
}
