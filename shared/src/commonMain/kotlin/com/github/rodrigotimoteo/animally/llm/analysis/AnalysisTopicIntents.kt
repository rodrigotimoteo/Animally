package com.github.rodrigotimoteo.animally.llm.analysis

/**
 * Detects the concrete data slices requested by an analysis question.
 * Keeping topic matching separate from the higher-level analysis gate makes
 * each decision small and keeps the public intent facade easy to extend.
 */
internal object AnalysisTopicIntents {
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

    fun hasAnalysisTopic(lowered: String): Boolean =
        weightRegex.containsMatchIn(lowered) ||
            careRegex.containsMatchIn(lowered) ||
            gestationRegex.containsMatchIn(lowered) ||
            overdueRegex.containsMatchIn(lowered)

    fun hasToolKeyword(query: String): Boolean = toolAnalysisRegex.containsMatchIn(query.lowercase())
}
