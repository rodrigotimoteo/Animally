package com.github.rodrigotimoteo.animally.domain.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VeterinaryWebQueryTest {
    @Test
    fun coreDiseaseRemainsRelevantWhenQuestionAddsSearchQualifiers() {
        val generalLaminitis =
            VeterinaryWebSource(
                sourceId = "msd:laminitis",
                title = "Laminitis in Horses",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/laminitis-in-horses",
                excerpt = "Laminitis is a painful condition affecting the hoof.",
            )
        val unrelated =
            VeterinaryWebSource(
                sourceId = "msd:equine-colic",
                title = "Colic in Horses",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/colic-in-horses",
                excerpt = "Colic can have many causes.",
            )

        assertEquals(
            listOf(generalLaminitis),
            VeterinaryWebQuery.filterRelevantSources(
                "What are the signs and causes of laminitis in horses?",
                listOf(unrelated, generalLaminitis),
            ),
        )
    }

    @Test
    fun multipleCoreSubjectsRemainStrict() {
        val ultrasound =
            VeterinaryWebSource(
                sourceId = "msd:ultrasound",
                title = "Ultrasound in mares",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/ultrasound",
                excerpt = "Ultrasound helps examine reproductive structures.",
            )
        val transrectal =
            VeterinaryWebSource(
                sourceId = "msd:transrectal",
                title = "Transrectal examination in mares",
                publisher = "MSD Veterinary Manual",
                url = "https://www.msdvetmanual.com/transrectal",
                excerpt = "Transrectal examination is used in reproductive work.",
            )

        assertEquals(
            emptyList<VeterinaryWebSource>(),
            VeterinaryWebQuery.filterRelevantSources(
                "What is transrectal ultrasound in mares?",
                listOf(ultrasound, transrectal),
            ),
        )
    }

    @Test
    fun `general laminitis question becomes a safe medical topic`() {
        assertEquals("laminitis horses", VeterinaryWebQuery.extractTopic("What is laminitis in horses?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("What is laminitis in horses?"))
    }

    @Test
    fun `common laminitis spelling is normalized for medical references`() {
        val question = "Can you tell me what laminites is?"

        assertEquals("laminitis", VeterinaryWebQuery.extractTopic(question))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion(question))
    }

    @Test
    fun `single transcription typo is normalized to the known medical term`() {
        val question = "What is an ultrasaund?"

        assertEquals("ultrasound", VeterinaryWebQuery.extractTopic(question))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion(question))
    }

    @Test
    fun `leishmaniasis variants become a safe medical topic`() {
        assertEquals("leishmaniasis", VeterinaryWebQuery.extractTopic("What is leishmaniasis?"))
        assertEquals("leishmaniasis", VeterinaryWebQuery.extractTopic("O que é leishmaniose?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("What is leishmaniasis?"))
        assertTrue(VeterinaryWebQuery.isMedicalQuestion("O que é leishmaniose?"))
    }

    @Test
    fun `leishmaniasis source survives trusted relevance filtering`() {
        val relevant =
            VeterinaryWebSource(
                sourceId = "pubmed:leishmaniasis",
                title = "Leishmania infection in domestic animals",
                publisher = "PubMed / Europe PMC",
                url = "https://pubmed.ncbi.nlm.nih.gov/leishmaniasis/",
                excerpt = "Leishmaniasis is a parasitic disease caused by Leishmania species.",
            )
        val unrelated =
            relevant.copy(
                sourceId = "pubmed:colic",
                title = "Colic in horses",
                url = "https://pubmed.ncbi.nlm.nih.gov/colic/",
                excerpt = "Colic is a common gastrointestinal emergency.",
            )

        assertEquals(
            listOf(relevant),
            VeterinaryWebQuery.filterRelevantSources(
                "O que é leishmaniose?",
                listOf(unrelated, relevant),
            ),
        )
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
