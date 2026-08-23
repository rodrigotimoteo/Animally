package com.github.rodrigotimoteo.animally.domain.export.pdf

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/**
 * Android implementation: paints the platform-neutral page models from
 * [layoutReport] with the framework `PdfDocument`, so the output matches the
 * iOS renderer op-for-op.
 */
actual fun generatePdf(report: PdfReportData): ByteArray {
    val pages = layoutReport(report)
    val document = PdfDocument()
    pages.forEachIndexed { index, page ->
        val pageInfo =
            PdfDocument.PageInfo
                .Builder(PdfTheme.PAGE_WIDTH.toInt(), PdfTheme.PAGE_HEIGHT.toInt(), index + 1)
                .create()
        val pdfPage = document.startPage(pageInfo)
        page.ops.forEach { op -> drawOp(pdfPage.canvas, op) }
        document.finishPage(pdfPage)
    }
    val output = ByteArrayOutputStream()
    document.writeTo(output)
    document.close()
    return output.toByteArray()
}

private fun drawOp(
    canvas: Canvas,
    op: PdfOp,
) {
    when (op) {
        is PdfOp.Rect -> canvas.drawRect(op.toRectF(), fillPaint(op.color))
        is PdfOp.Text -> drawText(canvas, op)
    }
}

private fun drawText(
    canvas: Canvas,
    op: PdfOp.Text,
) {
    val paint = textPaint(op.size, op.bold, op.color)
    val textWidth = paint.measureText(op.text)
    val x =
        when {
            op.centered -> op.x - textWidth / 2
            op.rightAligned -> op.x - textWidth
            else -> op.x
        }
    // Ops carry the top of the text box; Canvas.drawText wants the baseline.
    val baseline = op.y - paint.fontMetrics.ascent
    canvas.drawText(op.text, x.toFloat(), baseline.toFloat(), paint)
}

private fun PdfOp.Rect.toRectF() = RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat())

private fun fillPaint(color: Long): Paint =
    Paint().apply {
        style = Paint.Style.FILL
        this.color = color.toInt()
    }

private fun textPaint(
    textSize: Double,
    bold: Boolean,
    color: Long,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toInt()
        this.textSize = textSize.toFloat()
        isFakeBoldText = bold
    }
