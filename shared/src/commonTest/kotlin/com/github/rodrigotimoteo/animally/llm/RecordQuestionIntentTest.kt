package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.llm.analysis.AnalysisTopicIntents
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordQuestionIntentTest {
    private val today = LocalDate(2026, 8, 24)

    @Test
    fun `period activity is a record question`() {
        val range = RagDateRangeIntent.resolve("What happened this month?", today)

        assertTrue(RecordQuestionIntent.isRecordQuestion("What happened this month?", null, range))
    }

    @Test
    fun `date words alone do not turn general knowledge into a record question`() {
        val range = RagDateRangeIntent.resolve("What is the weather today?", today)

        assertFalse(RecordQuestionIntent.isRecordQuestion("What is the weather today?", null, range))
    }

    @Test
    fun `patient counts are strict record questions`() {
        assertTrue(RecordQuestionIntent.isRecordQuestion("How many patients do I have?", null, null))
    }

    @Test
    fun `portuguese record activity is strict`() {
        val query = "O que aconteceu este mês?"
        val range = RagDateRangeIntent.resolve(query, today)

        assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, range))
    }

    @Test
    fun `portuguese general definition stays general`() {
        assertFalse(RecordQuestionIntent.isRecordQuestion("O que é uma vacinação?", null, null))
    }

    @Test
    fun `definition of pregnancy remains a general question`() {
        assertFalse(RecordQuestionIntent.isRecordQuestion("What is pregnancy?", null, null))
    }

    @Test
    fun `title-cased clinical explanation is recognized as educational`() {
        assertTrue(RecordQuestionIntent.isEducationalQuestion("Can you explain Equine Metabolic Syndrome?"))
        assertTrue(RecordQuestionIntent.isEducationalQuestion("O que é Síndrome Metabólica Equina?"))
        assertFalse(RecordQuestionIntent.isEducationalQuestion("Is Storm pregnant?"))
    }

    @Test
    fun `individual portuguese horse reference is a record question`() {
        assertTrue(RecordQuestionIntent.isRecordQuestion("O meu cavalo está prenhe?", null, null))
    }

    @Test
    fun `plural they reference does not create an individual patient scope`() {
        assertFalse(
            RecordQuestionIntent.hasIndividualPatientReference(
                "Which mares are currently pregnant, how many days along are they, and when are they due?",
            ),
        )
        assertTrue(RecordQuestionIntent.hasIndividualPatientReference("What did she receive?"))
    }

    @Test
    fun `population pregnancy question is a record question without a pronoun`() {
        assertTrue(RecordQuestionIntent.isRecordQuestion("Which mares are pregnant?", null, null))
        assertTrue(
            RecordQuestionIntent.isRecordQuestion(
                "Which mares are currently pregnant, how many days along are they, and when are they due?",
                null,
                null,
            ),
        )
        assertFalse(RecordQuestionIntent.isRecordQuestion("What is pregnancy?", null, null))
    }

    @Test
    fun `general questions with title-cased external entities stay general`() {
        val queries =
            listOf(
                "Who wrote Pride and Prejudice?",
                "What is the history of Portugal?",
                "Can you explain Equine Dentistry?",
                "How do I care for a horse?",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isGeneralKnowledgeQuestion(query), query)
            assertFalse(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
        }
    }

    @Test
    fun `generic husbandry questions with possessive horse wording stay general`() {
        val queries =
            listOf(
                "What should I feed my horse?",
                "How should I care for my horse?",
                "Can my horse eat carrots?",
                "What is a good diet for my horse?",
                "How much water should I give my horse?",
                "How many hours of turnout does my horse need?",
                "Como devo alimentar o meu cavalo?",
                "Quanto de água devo dar ao meu cavalo?",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isGeneralKnowledgeQuestion(query), query)
            assertFalse(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
        }
    }

    @Test
    fun `past quantity wording remains a record lookup`() {
        val query = "How much water did my horse receive?"

        assertFalse(RecordQuestionIntent.isGeneralKnowledgeQuestion(query))
        assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null))
    }

    @Test
    fun `typed patient lookup recognizes lowercase unknown names but not generic horse words`() {
        assertTrue(RecordQuestionIntent.isRecordQuestion("is storm pregnant?", null, null))
        assertTrue(RecordQuestionIntent.hasLikelyNamedPatientReference("is storm pregnant?"))
        assertTrue(RecordQuestionIntent.isRecordQuestion("What vaccinations did storm receive?", null, null))
        assertTrue(RecordQuestionIntent.hasLikelyNamedPatientReference("What vaccinations did storm receive?"))

        assertFalse(RecordQuestionIntent.isRecordQuestion("Is a horse pregnant?", null, null))
        assertFalse(RecordQuestionIntent.hasLikelyNamedPatientReference("Is a horse pregnant?"))
        assertFalse(RecordQuestionIntent.hasLikelyNamedPatientReference("What vaccinations did she receive?"))
    }

    @Test
    fun `named horse phrasing stays a record lookup even when it starts with what is`() {
        val query = "What is the vaccination date for a horse named Pegasus?"

        assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null))
        assertFalse(RecordQuestionIntent.isGeneralKnowledgeQuestion(query))
        assertTrue(RecordQuestionIntent.hasLikelyNamedPatientReference(query))
    }

    @Test
    fun `natural patient inventory questions stay record lookups`() {
        val queries =
            listOf(
                "What horses do I have?",
                "Which horses do I have?",
                "Do I have any horses?",
                "List my horses",
                "Que cavalos tenho?",
                "Mostra os meus cavalos",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertFalse(RecordQuestionIntent.isGeneralKnowledgeQuestion(query), query)
        }
    }

    @Test
    fun `typed list questions stay grounded in the record corpus`() {
        val queries =
            listOf(
                "List vaccinations",
                "List medications",
                "Lista as vacinas",
                "Liste os medicamentos",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertFalse(RecordQuestionIntent.isGeneralKnowledgeQuestion(query), query)
        }
    }

    @Test
    fun `named patient identity questions stay grounded even without a typed record`() {
        val queries =
            listOf(
                "How old is Pegasus?",
                "What breed is Pegasus?",
                "Qual é a idade do Pegasus?",
                "Qual é a raça do Pegasus?",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertTrue(RecordQuestionIntent.hasLikelyNamedPatientReference(query), query)
        }
    }

    @Test
    fun `expanded patient identity fields stay grounded`() {
        val queries =
            listOf(
                "What sex is Pegasus?",
                "What species is Pegasus?",
                "What is Pegasus's UELN?",
                "Where is Pegasus stabled?",
                "Qual é a cor do Pegasus?",
            )

        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertTrue(RecordQuestionIntent.hasLikelyNamedPatientReference(query), query)
        }
    }

    @Test
    fun `expanded record type vocabulary keeps typed lookups grounded`() {
        assertTrue(RecordTypeIntent.expectedRecordTypes("What was Bella's last surgery?").contains("SURGERY"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("Show Thunder's lab results").contains("LAB_RESULT"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("What did Lua's blood work show?").contains("LAB_RESULT"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("Open the imaging record").contains("IMAGING"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("Which reminders are due?").contains("CUSTOM_REMINDER"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("What is the embryo transfer date?").contains("EMBRYO_TRANSFER"))
        assertTrue(RecordTypeIntent.expectedRecordTypes("When was the ICSI procedure?").contains("ICSI"))
        assertEquals(
            setOf("GESTATION", "REPRODUCTION_EVENT"),
            RecordTypeIntent.expectedRecordTypes("How long ago was Descarada bred?"),
        )
        assertEquals(
            setOf("GESTATION", "REPRODUCTION_EVENT"),
            RecordTypeIntent.expectedRecordTypes("What did Thunder's pregnancy check show?"),
        )
        assertEquals(
            setOf("LAB_RESULT", "PATIENT"),
            RecordTypeIntent.expectedRecordTypes("What is Thunder's Coggins result and expiry?"),
        )
        assertTrue(RecordTypeIntent.expectedRecordTypes("What chronic conditions are recorded for Orion?").contains("ANAMNESE"))
        assertEquals(
            setOf("REPRODUCTION_EVENT"),
            RecordTypeIntent.expectedRecordTypes("Which stallion was used to breed Lua?"),
        )
    }

    @Test
    fun `breeding outcome is a current reproductive question`() {
        assertTrue(AnalysisTopicIntents.wantsBreedingOutcome("What is Brisa's breeding outcome?"))
        assertTrue(AnalysisTopicIntents.wantsCurrentGestation("What is Brisa's breeding outcome?"))
    }
}
