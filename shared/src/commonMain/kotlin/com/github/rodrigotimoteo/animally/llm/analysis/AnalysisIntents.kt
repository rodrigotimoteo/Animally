package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.llm.RecordQuestionIntent

/**
 * Deterministic intent detection for analysis-mode summaries. Conservative on
 * purpose: only count/list/aggregate phrasings trigger the repository scan -
 * ordinary retrieval questions must not pay the extra context cost. English
 * and Portuguese phrasings are covered because the assistant mirrors the
 * user's language per turn.
 */
object AnalysisIntents {
    private val analysisRegex =
        Regex(
            "\\b(how many|how much has|how much have|average|trend|when was the last|which patients|total)\\b|" +
                "\\b(?:what|which)\\s+(?:patients?|horses?|mares?)\\s+do\\s+(?:i|we)\\s+have\\b|" +
                "\\bdo\\s+(?:i|we)\\s+have\\s+(?:any\\s+)?(?:patients?|horses?|mares?)\\b|" +
                "\\b(?:list|show)\\s+(?:my|our|the)\\s+(?:patients?|horses?|mares?)\\b|" +
                "\\b(quantos|quantas|quanto|média|media|tendência|tendencia|" +
                "quando foi a última|quando foi a ultima|quais pacientes|" +
                "(?:que|quais)\\s+(?:pacientes?|cavalos?|éguas?|eguas?)\\s+(?:tenho|temos)|" +
                "(?:mostra|liste|lista)\\s+(?:os|as)?\\s*(?:meus|minhas|nossos|nossas)?\\s*" +
                "(?:pacientes?|cavalos?|éguas?|eguas?))\\b",
        )

    private val datasetReferenceRegex =
        Regex(
            "\\b(my|our|your)\\s+(patients?|horses?|mares?|records?|data|dataset|" +
                "weights?|vaccinations?|gestations?|care|history|timeline)\\b|" +
                "\\b(in|from|across|between|within)\\s+(?:my|our|the)\\s+" +
                "(records?|data|dataset|patients?|horses?|mares?|history|timeline)\\b|" +
                "\\b(?:meus|minhas|nossos|nossas)\\s+(pacientes?|cavalos?|éguas?|eguas?)\\b|" +
                "\\b(?:nos|nas)\\s+(?:meus|minhas|nossos|nossas)\\s+" +
                "(registos?|dados|pacientes?|cavalos?|éguas?|eguas?)\\b|" +
                "\\b(records?|dataset|data|statistics?|statistical|analysis|" +
                "compare|comparison|correlation|distribution|regression|outliers?|" +
                "by\\s+month|per\\s+month|over\\s+time|por\\s+m[eê]s|" +
                "por\\s+raça|por\\s+esp[eé]cie|dados|estatística|estatistica)\\b",
        )
    private val recordAnalysisTopicRegex =
        Regex(
            "\\b(overdue|upcoming|reminders?|" +
                "assistência|assistencia|atrasad[oa]s?|pendentes?|vencid[oa]s?)\\b",
        )
    private val careAnalysisReferenceRegex =
        Regex(
            "\\b(care|cuidados?)\\s+(?:records?|data|dataset|history|timeline|" +
                "registos?|dados|histórico|historico)\\b",
        )

    fun isAnalysisQuery(query: String): Boolean {
        val lowered = query.lowercase()
        val isGeneralQuestion = RecordQuestionIntent.isGeneralKnowledgeQuestion(query)
        val hasScope = hasRecordAnalysisScope(query, lowered)
        return !isGeneralQuestion &&
            hasScope &&
            (analysisRegex.containsMatchIn(lowered) || AnalysisTopicIntents.hasAnalysisTopic(lowered))
    }

    fun requiresTools(query: String): Boolean =
        isAnalysisQuery(query) &&
            AnalysisTopicIntents.hasToolKeyword(query)

    private fun hasRecordAnalysisScope(
        query: String,
        lowered: String,
    ): Boolean =
        datasetReferenceRegex.containsMatchIn(lowered) ||
            RecordQuestionIntent.hasIndividualPatientReference(query) ||
            RecordQuestionIntent.hasLikelyNamedPatientReference(query) ||
            RecordQuestionIntent.hasGestationPopulationReference(query) ||
            RecordQuestionIntent.isRecordQuestion(query, null, null) ||
            recordAnalysisTopicRegex.containsMatchIn(lowered) ||
            careAnalysisReferenceRegex.containsMatchIn(lowered)
}
