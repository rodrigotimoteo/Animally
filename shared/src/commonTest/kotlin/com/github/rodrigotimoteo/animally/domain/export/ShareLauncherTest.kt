package com.github.rodrigotimoteo.animally.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareLauncherTest {
    @Test
    fun `sanitizes path traversal to a single filename component`() {
        assertEquals("report.csv", sanitizeShareFileName("../../report.csv"))
        assertEquals("report.csv", sanitizeShareFileName("..\\report.csv"))
        assertEquals("bar.pdf", sanitizeShareFileName("reports/../bar.pdf"))
    }

    @Test
    fun `replaces unsafe filename characters and uses fallback for empty names`() {
        assertEquals("report_name_.pdf", sanitizeShareFileName("report:name?.pdf"))
        assertEquals("export", sanitizeShareFileName("../"))
        assertEquals("export", sanitizeShareFileName(".."))
    }
}
