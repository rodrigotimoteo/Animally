package com.github.rodrigotimoteo.animally.llm.prompts

/**
 * Language detection extracted from AssistantPrompts.
 * Determines whether a query reads as Portuguese so the assistant can
 * mirror the user's language per turn.
 */
internal object LanguageDetector {
    private const val PORTUGUESE_VACCINATION = "vacinação"
    private const val PORTUGUESE_DEWORMING = "desparasitação"

    private val PORTUGUESE_MARKERS =
        setOf(
            "quantos",
            "quanto",
            "quando",
            "quem",
            "onde",
            "qual",
            "quais",
            "como",
            "porque",
            "porquê",
            "tenho",
            "tem",
            "cavalo",
            "cavalos",
            "égua",
            "paciente",
            "pacientes",
            "vacina",
            "vacinas",
            PORTUGUESE_VACCINATION,
            "tratamento",
            "tratamentos",
            "gestação",
            "peso",
            "registo",
            "registos",
            "este",
            "esta",
            "neste",
            "nesta",
            "mês",
            "mes",
            "semana",
            "ano",
            "hoje",
            "ontem",
            "aconteceu",
            "ocorreu",
            "atividade",
            "último",
            "última",
            "ultimo",
            "ultima",
            "ferrador",
            "ferragem",
            PORTUGUESE_VACCINATION,
            PORTUGUESE_DEWORMING,
            "é",
        )

    private val portugueseDiacriticRegex = Regex("[áâãàçéêíóôõú]")

    private val englishQuestionCueRegex =
        Regex(
            "\\b(?:what|when|which|who|where|how|why|is|are|was|were|did|do|does|can|could|would|should|" +
                "tell|explain|please|give|show|list|compare|describe|summari[sz]e|analyse|analyze)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val portugueseQuestionMarkers =
        setOf(
            "que",
            "qual",
            "quais",
            "quanto",
            "quantos",
            "quantas",
            "quando",
            "quem",
            "onde",
            "como",
            "porque",
            "porquê",
            "tenho",
            "tem",
            "teve",
            "está",
            "esta",
            "estão",
            "estao",
            "há",
            "ha",
            "foi",
            "pode",
            "podes",
            "poderia",
            "é",
            "são",
            "sao",
            "recebeu",
            "registado",
            "registada",
            "aconteceu",
            "ocorreu",
        )

    /**
     * True when [query] reads as Portuguese. Question grammar wins over
     * accents in proper names, so a name such as "Inês" does not switch an
     * otherwise English turn.
     */
    fun isPortugueseQuery(query: String): Boolean {
        val tokens =
            query
                .split(Regex("\\s+"))
                .map(FtsQueryBuilder::clean)
                .map(String::lowercase)
        val hasPortugueseQuestionFrame = tokens.any { it in portugueseQuestionMarkers }
        if (hasPortugueseQuestionFrame) return true
        if (englishQuestionCueRegex.containsMatchIn(query)) return false
        return portugueseDiacriticRegex.containsMatchIn(query) ||
            tokens.any { it in PORTUGUESE_MARKERS }
    }
}
