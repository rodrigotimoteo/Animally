package com.github.rodrigotimoteo.animally.presentation.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordDetailAttachmentTest {
    @Test
    fun `detail attachments preserve paths and derive file names`() {
        val attachments =
            recordDetailAttachments(
                " /Documents/attachments/scan-one.jpg, file:///Documents/attachments/scan-two.png,, ",
            )

        assertEquals(2, attachments.size)
        assertEquals("/Documents/attachments/scan-one.jpg", attachments[0].path)
        assertEquals("scan-one.jpg", attachments[0].fileName)
        assertEquals("file:///Documents/attachments/scan-two.png", attachments[1].path)
        assertEquals("scan-two.png", attachments[1].fileName)
    }

    @Test
    fun `detail attachments are empty when image field is blank`() {
        assertEquals(emptyList(), recordDetailAttachments(null))
        assertEquals(emptyList(), recordDetailAttachments(" ,  "))
    }
}
