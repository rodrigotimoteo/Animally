package com.github.rodrigotimoteo.animally.llm

import kotlinx.datetime.LocalDate
import kotlin.test.Test
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
}
