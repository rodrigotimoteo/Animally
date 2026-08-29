package com.github.rodrigotimoteo.animally.llm

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A deliberately broad, deterministic intent matrix. These are not model
 * quality scores: they protect the boundary that decides whether the app may
 * search private records or ask the cloud model for an aggregate.
 */
class AssistantQualityMatrixTest {
    @Test
    fun `general education matrix never searches patient records or analysis tools`() {
        val topics =
            listOf(
                "equine metabolic syndrome",
                "colic",
                "laminitis",
                "ulcers in horses",
                "horse nutrition",
                "equine dentistry",
                "vaccination schedules",
                "deworming",
                "gestation",
                "foaling",
                "horse biomechanics",
                "tendon injuries",
                "equine influenza",
                "lameness",
                "horse welfare",
                "antibiotic resistance",
                "ultrasound imaging",
                "mare fertility",
                "foal development",
                "pasture management",
                "equine first aid",
                "body condition scoring",
            )
        val englishTemplates =
            listOf(
                "What is %s?",
                "Can you explain %s?",
                "Explain %s in simple terms.",
                "Tell me about %s.",
                "How does %s work?",
                "Why does %s happen?",
            )
        val portugueseTopics =
            listOf(
                "síndrome metabólica equina",
                "cólica",
                "laminite",
                "úlceras em cavalos",
                "nutrição equina",
                "odontologia equina",
                "vacinação",
                "desparasitação",
                "gestação",
                "parto",
                "claudicação",
                "fertilidade da égua",
            )
        val portugueseTemplates =
            listOf(
                "O que é %s?",
                "Podes explicar %s?",
                "Fala-me sobre %s.",
                "Como funciona %s?",
                "Porque é que %s acontece?",
            )
        val queries =
            englishTemplates.flatMap { template -> topics.map { topic -> template.replace("%s", topic) } } +
                portugueseTemplates.flatMap { template -> portugueseTopics.map { topic -> template.replace("%s", topic) } }

        assertTrue(queries.size >= 180, "matrix unexpectedly shrank: ${queries.size}")
        queries.forEach { query ->
            assertTrue(RecordQuestionIntent.isGeneralKnowledgeQuestion(query), query)
            assertFalse(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertFalse(AnalysisIntents.isAnalysisQuery(query), query)
        }
    }

    @Test
    fun `record and analysis matrix stays grounded to app data`() {
        val recordQueries =
            listOf(
                "Which patients do I have?",
                "How many patients do I have?",
                "What happened this month?",
                "What vaccinations did she receive?",
                "What medication was given to Luna?",
                "Tell me about Bella's ultrasound record.",
                "Show me Thunder's weight history.",
                "Which mares are currently pregnant?",
                "Is Storm's pregnancy active?",
                "When was Ghost's last farrier visit?",
                "How much metronidazole was given?",
                "O que aconteceu este mês?",
                "Que vacina recebeu ela?",
                "Que medicamento recebeu a Lua?",
                "Mostra o histórico de peso da Bella.",
                "Quais éguas estão prenhes?",
                "Quando foi a última visita do ferrador da Ghost?",
            )
        recordQueries.forEach { query ->
            assertTrue(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
        }

        val analysisQueries =
            listOf(
                "Analyze the average weight in my records.",
                "Compare vaccinations by month.",
                "How many vaccinations are in my records?",
                "What is the median weight across my horses?",
                "Show the weight trend over time.",
                "How many active pregnancies are in my records?",
                "Which patients have overdue care?",
                "Faz uma análise dos dados de peso.",
                "Compara as vacinações por mês.",
                "Qual é a média de peso dos meus cavalos?",
                "Quantas gestações ativas tenho nos registos?",
                "Que cuidados estão atrasados?",
            )
        analysisQueries.forEach { query ->
            assertTrue(AnalysisIntents.isAnalysisQuery(query), query)
        }
    }

    @Test
    fun `general unrelated matrix remains outside app analysis`() {
        val queries =
            listOf(
                "Why is the sky blue?",
                "How does a black hole form?",
                "What is the average temperature in Lisbon?",
                "Tell me about the history of Portugal.",
                "Can you explain photosynthesis?",
                "What causes tides?",
                "How does a battery work?",
                "Who wrote Pride and Prejudice?",
                "What is a good recipe for soup?",
                "How do I learn Spanish?",
                "What are the latest football rules?",
                "Why do leaves change colour?",
            )
        queries.forEach { query ->
            assertFalse(RecordQuestionIntent.isRecordQuestion(query, null, null), query)
            assertFalse(AnalysisIntents.isAnalysisQuery(query), query)
        }
    }
}
