@file:OptIn(ExperimentalForeignApi::class)

package com.github.rodrigotimoteo.animally.domain.export.pdf

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import platform.posix.memcpy

/**
 * iOS implementation: paints the platform-neutral page models from
 * [layoutReport] with `UIGraphicsPDFRenderer` + CoreGraphics, so the output
 * matches the Android renderer op-for-op.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun generatePdf(report: PdfReportData): ByteArray {
    val pages = layoutReport(report)
    val renderer =
        UIGraphicsPDFRenderer(
            bounds = CGRectMake(0.0, 0.0, PdfTheme.PAGE_WIDTH, PdfTheme.PAGE_HEIGHT),
            format = UIGraphicsPDFRendererFormat(),
        )
    val data =
        renderer.PDFDataWithActions { context ->
            val pdfContext = requireNotNull(context)
            val cgContext = requireNotNull(pdfContext.CGContext)
            pages.forEach { page ->
                pdfContext.beginPage()
                page.ops.forEach { op -> drawOp(cgContext, op) }
            }
        }
    return data.toByteArray()
}

private fun drawOp(
    context: CGContextRef,
    op: PdfOp,
) {
    when (op) {
        is PdfOp.Rect -> {
            CGContextSetRGBFillColor(
                context,
                PdfTheme.red(op.color),
                PdfTheme.green(op.color),
                PdfTheme.blue(op.color),
                PdfTheme.alpha(op.color),
            )
            CGContextFillRect(context, CGRectMake(op.x, op.y, op.width, op.height))
        }

        is PdfOp.Text -> drawText(op)
    }
}

private fun drawText(op: PdfOp.Text) {
    val font =
        if (op.bold) {
            UIFont.boldSystemFontOfSize(op.size)
        } else {
            UIFont.systemFontOfSize(op.size)
        }
    val attributes =
        mapOf<Any?, Any>(
            NSFontAttributeName to font,
            NSForegroundColorAttributeName to uiColor(op.color),
        )
    val text = op.text as NSString
    val textWidth = text.sizeWithAttributes(attributes).useContents { width }
    val x =
        when {
            op.centered -> op.x - textWidth / 2
            op.rightAligned -> op.x - textWidth
            else -> op.x
        }
    // UIKit's flipped coordinate space draws from the top-left of the box,
    // which is exactly what the ops carry.
    text.drawAtPoint(point = CGPointMake(x, op.y), withAttributes = attributes)
}

private fun uiColor(color: Long): UIColor =
    UIColor(
        red = PdfTheme.red(color),
        green = PdfTheme.green(color),
        blue = PdfTheme.blue(color),
        alpha = PdfTheme.alpha(color),
    )

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
