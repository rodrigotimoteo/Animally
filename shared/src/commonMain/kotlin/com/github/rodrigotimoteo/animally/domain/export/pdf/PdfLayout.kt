package com.github.rodrigotimoteo.animally.domain.export.pdf

/**
 * Builds the full page models for [report]: branded header band on every
 * page, demographics grid, one aligned table per non-empty section, and a
 * "Page X of Y" footer. Both platform renderers paint these ops verbatim,
 * which keeps the documents visually identical.
 *
 * Facade over the split layout engine — pagination lives in [paginate],
 * chrome in [headerBandOps]/[footerOps], and section rendering in
 * [TableBlock]/[RecordCardBlock]. No layout logic here, just page assembly.
 */
internal fun layoutReport(report: PdfReportData): List<PdfPageModel> {
    val generatedAt = generationStamp()
    val contentOps = paginate(report)
    val totalPages = contentOps.size
    return contentOps.mapIndexed { index, ops ->
        val pageOps =
            buildList {
                addAll(headerBandOps(generatedAt, report.palette))
                addAll(ops)
                addAll(footerOps(index + 1, totalPages, generatedAt))
            }
        PdfPageModel(
            pageNumber = index + 1,
            totalPages = totalPages,
            ops = pageOps,
        )
    }
}
