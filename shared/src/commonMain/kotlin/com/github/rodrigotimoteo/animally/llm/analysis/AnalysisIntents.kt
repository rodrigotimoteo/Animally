package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.llm.RecordQuestionIntent

/**
 * Deterministic intent detection for analysis-mode summaries. Conservative on
 * purpose: only count/list/aggregate phrasings trigger the repository scan -
 * ordinary retrieval questions must not pay the extra context cost. English
 * and Portuguese phrasings are covered because the assistant mirrors the
 * user's language per turn.
 */
@Suppress("TooManyFunctions")
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

    private val censusRegex = Regex("\\b(patients|horses|pacientes|cavalos|égua|éguas)\\b")
    private val weightRegex =
        Regex(
            "\\b(weight|weights|weigh|weighs|weighed|weighing|peso|pesos|pesa|pesam|" +
                "pesada|pesado|pesagem)\\b",
        )

    private val careRegex =
        Regex(
            "\\b(vaccinations?|vaccines?|boosters?|dewormings?|dewormed|dewormer|farriers?|shod|shoeing|trims?|" +
                "vacinações?|vacinacoes?|vacinas?|desparasitações?|desparasitacoes?|" +
                "ferrageamentos?|ferrador|ferragem|care|cuidados?)\\b",
        )

    private val lastDoneRegex =
        Regex(
            "\\b(when was the last|quando foi a última|quando foi a ultima|" +
                "qual foi a última|qual foi a ultima|qual foi o último|qual foi o ultimo)\\b",
        )

    private val gestationRegex =
        Regex(
            "\\b(pregnant|gestations?|foaling|in foal|bred|breeding|prenha|prenhe|prenhes|" +
                "prenhez|gravidez|gestação|gestacao|gestações|gestacoes|parição|" +
                "paricao|parições|paricoes)\\b",
        )

    private val overdueRegex =
        Regex("\\b(overdue|due|upcoming|reminders?|atrasad[oa]s?|pendentes?|vencid[oa]s?)\\b")

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

    private val currentGestationRegex =
        Regex(
            "\\b(pregnant|pregnancy|pregnancies|in\\s+foal|days?\\s+along|gestation\\s+day|" +
                "due\\s+date|expected\\s+foaling|foaling\\s+date|pregnancy\\s+status|" +
                "current(?:ly)?\\s+(?:pregnan|pregnancy|gestation)|" +
                "prenha|prenhe|prenhes|prenhez|dia[s]?\\s+de\\s+gestação|dia[s]?\\s+de\\s+gestacao|" +
                "parto\\s+previsto|data\\s+do\\s+parto|parição|paricao)\\b",
        )

    private val breedingOutcomeRegex =
        Regex(
            "\\b(breeding\\s+(?:outcome|result)|outcome\\s+of\\s+(?:the\\s+)?breeding|" +
                "resultado\\s+(?:da\\s+)?cobertura|resultado\\s+reprodutivo|" +
                "desfecho\\s+(?:da\\s+)?cobertura)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val breedingTimingRegex =
        Regex(
            "\\b(bred|breeding\\s+date|date\\s+(?:was\\s+)?bred|" +
                "when\\s+was\\s+[^?]+\\s+bred|how\\s+long\\s+ago\\s+[^?]+\\s+bred|" +
                "coberta|data\\s+da\\s+cobertura|quando\\s+foi\\s+coberta|" +
                "há\\s+quanto\\s+tempo\\s+[^?]+\\s+coberta)\\b",
        )

    private val toolAnalysisRegex =
        Regex(
            "\\b(analy[sz]e|analysis|dataset|statistics?|statistical|average|mean|trend|" +
                "compare|comparison|correlat|" +
                "percentage|proportion|distribution|median|variance|regression|outlier|" +
                "across|between|by month|per month|by breed|by species|over time|pattern|relationship|" +
                "média|media|analisar|análise|analise|dados|estatística|estatistica|comparar|" +
                "correlação|correlacao|percentagem|proporção|proporcao|distribuição|" +
                "mediana|variância|variancia|tendência|tendencia|padrão|padrao|relação|relacao|" +
                "por mês|por mes|por raça|por raca|por espécie|por especie)\\b",
        )

    fun isAnalysisQuery(query: String): Boolean {
        val lowered = query.lowercase()
        val isGeneralQuestion = RecordQuestionIntent.isGeneralKnowledgeQuestion(query)
        val hasScope = hasRecordAnalysisScope(query, lowered)
        return !isGeneralQuestion &&
            hasScope &&
            (analysisRegex.containsMatchIn(lowered) || hasAnalysisTopic(lowered))
    }

    fun wantsCensus(query: String): Boolean {
        val lowered = query.lowercase()
        if (censusRegex.containsMatchIn(lowered)) return true
        return !hasAnalysisTopic(lowered)
    }

    fun wantsWeight(query: String): Boolean = weightRegex.containsMatchIn(query.lowercase())

    fun wantsCareCounts(query: String): Boolean {
        val lowered = query.lowercase()
        return careRegex.containsMatchIn(lowered) || lastDoneRegex.containsMatchIn(lowered)
    }

    fun wantsGestation(query: String): Boolean = gestationRegex.containsMatchIn(query.lowercase())

    fun wantsCurrentGestation(query: String): Boolean {
        val lowered = query.lowercase()
        return currentGestationRegex.containsMatchIn(lowered) ||
            breedingTimingRegex.containsMatchIn(lowered) ||
            breedingOutcomeRegex.containsMatchIn(lowered)
    }

    fun wantsBreedingTiming(query: String): Boolean = breedingTimingRegex.containsMatchIn(query.lowercase())

    fun wantsBreedingOutcome(query: String): Boolean = breedingOutcomeRegex.containsMatchIn(query.lowercase())

    fun wantsOverdue(query: String): Boolean = overdueRegex.containsMatchIn(query.lowercase())

    fun requiresTools(query: String): Boolean =
        isAnalysisQuery(query) &&
            toolAnalysisRegex.containsMatchIn(query.lowercase())

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

private fun hasAnalysisTopic(lowered: String): Boolean =
    AnalysisIntents.wantsWeight(lowered) ||
        AnalysisIntents.wantsCareCounts(lowered) ||
        AnalysisIntents.wantsGestation(lowered) ||
        AnalysisIntents.wantsOverdue(lowered)
