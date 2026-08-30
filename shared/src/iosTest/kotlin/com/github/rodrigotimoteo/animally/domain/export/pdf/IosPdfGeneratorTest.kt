package com.github.rodrigotimoteo.animally.domain.export.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosPdfGeneratorTest {
    @Test
    fun `minimal report renders a valid pdf payload`() {
        val bytes =
            generatePdf(
                PdfReportData(
                    patient = PdfPatient(name = "Lua do Pinhal", species = "Equine"),
                    sections = emptyList(),
                ),
            )

        assertTrue(bytes.size > PDF_SIGNATURE.length)
        assertEquals(PDF_SIGNATURE, bytes.copyOfRange(0, PDF_SIGNATURE.length).decodeToString())
    }

    private companion object {
        const val PDF_SIGNATURE = "%PDF"
    }
}
