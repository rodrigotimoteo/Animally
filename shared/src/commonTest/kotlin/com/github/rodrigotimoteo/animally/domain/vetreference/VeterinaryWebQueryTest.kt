package com.github.rodrigotimoteo.animally.domain.vetreference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VeterinaryWebQueryTest {
    @Test
    fun `general laminitis question becomes a safe medical topic`() {
        assertEquals("laminitis horses", VeterinaryWebQuery.extractTopic("What is laminitis in horses?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("What is laminitis in horses?"))
    }

    @Test
    fun `portuguese medical question is supported`() {
        assertEquals("laminite cavalos", VeterinaryWebQuery.extractTopic("O que é laminite em cavalos?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("O que é laminite em cavalos?"))
    }

    @Test
    fun `patient and owner shaped questions are not sent to public search`() {
        assertNull(VeterinaryWebQuery.extractTopic("What is my horse's laminitis?"))
        assertNull(VeterinaryWebQuery.extractTopic("What is Descarada's laminitis?"))
        assertFalse(VeterinaryWebQuery.isMedicalQuestion("What is my horse's laminitis?"))
    }

    @Test
    fun `non-medical questions do not trigger the reference path`() {
        assertNull(VeterinaryWebQuery.extractTopic("What is the weather today?"))
        assertFalse(VeterinaryWebQuery.isMedicalQuestion("What is the weather today?"))
    }
}
