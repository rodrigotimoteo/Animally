package com.github.rodrigotimoteo.animally.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Host-JVM edge-case tests for [CsvFormatter] RFC 4180 escaping and header
 * mapping. Full-document behavior is covered by commonTest's CsvExporterTest;
 * these target the formatter primitives directly.
 */
class CsvFormatterEdgeCasesTest {
    private val formatter = CsvFormatter

    @Test
    fun givenNullFieldWhenEscapedThenEmptyStringEmitted() {
        assertEquals("", formatter.escape(null))
    }

    @Test
    fun givenPlainFieldWhenEscapedThenUnchanged() {
        assertEquals("Thunder", formatter.escape("Thunder"))
    }

    @Test
    fun givenCarriageReturnOnlyFieldWhenEscapedThenQuoted() {
        assertEquals("\"colic\rwatch\"", formatter.escape("colic\rwatch"))
    }

    @Test
    fun givenQuoteOnlyFieldWhenEscapedThenDoubledAndWrapped() {
        // A single double-quote becomes four quotes: two wrap + two doubled content.
        assertEquals("\"\"\"\"", formatter.escape("\""))
    }

    @Test
    fun givenFieldsWithNullMiddleWhenLineRenderedThenNullBecomesEmptyCell() {
        assertEquals("a,,b\r\n", formatter.line(listOf("a", null, "b")))
    }

    @Test
    fun givenAnyFieldsWhenLineRenderedThenCrlfTerminatorAppended() {
        assertEquals("1,2\r\n", formatter.line(listOf(1, 2)))
    }

    @Test
    fun givenPascalCaseFieldWithoutOverrideWhenMappedThenSplitAtCamelBoundaries() {
        assertEquals("Next Visit Date", formatter.displayHeader("NextVisitDate"))
        assertEquals("General History", formatter.displayHeader("GeneralHistory"))
    }

    @Test
    fun givenAcronymFieldWithoutOverrideWhenMappedThenPassesThroughUnchanged() {
        assertEquals("UELN", formatter.displayHeader("UELN"))
    }

    @Test
    fun givenOverrideFieldWhenMappedThenDisplayFormWins() {
        assertEquals("Owner", formatter.displayHeader("OwnerId"))
        assertEquals("Trim/Shoe", formatter.displayHeader("TrimOrShoe"))
    }
}
