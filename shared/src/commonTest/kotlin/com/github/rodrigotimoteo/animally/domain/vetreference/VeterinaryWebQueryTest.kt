package com.github.rodrigotimoteo.animally.domain.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
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
        assertEquals("laminitis horses", VeterinaryWebQuery.extractTopic("O que é laminite em cavalos?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("O que é laminite em cavalos?"))
    }

    @Test
    fun `reproductive ultrasound question becomes a safe medical topic`() {
        assertEquals(
            "ultrasound transrectal",
            VeterinaryWebQuery.extractTopic("O que é uma ecografia transrectal?"),
        )
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("O que é uma ecografia transrectal?"))
    }

    @Test
    fun `reputable but unrelated sources are rejected`() {
        val relevant =
            VeterinaryWebSource(
                sourceId = "msd:transrectal-ultrasound",
                title = "Transrectal ultrasonography in mares",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/transrectal-ultrasound",
                excerpt = "Transrectal ultrasonography is used to examine the mare's reproductive tract.",
            )
        val unrelated =
            VeterinaryWebSource(
                sourceId = "msd:calf-pneumonia",
                title = "Enzootic Pneumonia of Calves",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/calf-pneumonia",
                excerpt = "The disease is caused by viral and bacterial pathogens.",
            )

        assertEquals(
            listOf(relevant),
            VeterinaryWebQuery.filterRelevantSources(
                "O que é uma ecografia transrectal?",
                listOf(unrelated, relevant),
            ),
        )
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
