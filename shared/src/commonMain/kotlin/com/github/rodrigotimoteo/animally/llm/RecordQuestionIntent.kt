package com.github.rodrigotimoteo.animally.llm

/**
 * Separates questions about the user's data from general veterinary
 * education. Cloud models may answer the latter, but never get to use cloud
 * flexibility as an excuse to fill a missing record with a guess.
 */
internal object RecordQuestionIntent {
    private val directRecordReferenceRegex =
        Regex(
            "\\b(my|our|your|active|current)\\s+(records?|patients?|horses?|history|timeline|data|dataset)\\b|" +
                "\\b(which|what)\\s+(patients?|records?|treatments?|visits?)\\b|" +
                "\\bhow\\s+many\\s+(patients?|horses?|records?|treatments?|visits?)\\b|" +
                "\\b(quantos|quantas)\\s+(pacientes?|cavalos?|registos?|tratamentos?|visitas?)\\b|" +
                "\\b(quais|que)\\s+(pacientes?|cavalos?|registos?|tratamentos?|visitas?)\\b|" +
                "\\b(on|in)\\s+(my|the)\\s+(records?|file|app|history|timeline)\\b|" +
                "\\bwhat\\s+(did|has|have|happened|occurred)\\b|" +
                "\\b(recent|latest|last|most\\s+recent)\\s+(record|history|activity|visit|treatment|care)\\b|" +
                "\\b(o\\s+que\\s+(aconteceu|ocorreu|foi\\s+registado)|" +
                "atividade\\s+recente|registos?\\s+recentes?)\\b|" +
                "\\b(últim[oa]s?|ultim[oa]s?|mais\\s+recente[s]?)\\s+" +
                "(registo[s]?|visita[s]?|tratamento[s]?|" +
                "vacina(?:ção|cao|ções|coes)?|consulta[s]?|atividade|ferragem)\\b|" +
                "\\b(nos|nas|no|na)\\s+(meus|minhas|os|as)?\\s*" +
                "(registos?|ficha|aplicação|aplicacao|histórico|historico|" +
                "linha\\s+do\\s+tempo)\\b",
        )
    private val patientPronounRegex =
        Regex(
            "\\b(she|he|her|his|they|their|ela|ele|dela|dele|" +
                "my\\s+(horse|patient|mare|gelding)|our\\s+(horse|patient|mare|gelding)|" +
                "your\\s+(horse|patient|mare|gelding)|the\\s+(horse|patient|mare|gelding)|" +
                "this\\s+(horse|patient|mare|gelding)|meu\\s+(cavalo|paciente)|" +
                "minha\\s+(égua|egua|paciente)|o\\s+meu\\s+(cavalo|paciente)|" +
                "a\\s+minha\\s+(égua|egua|paciente)|este\\s+(horse|patient|cavalo|paciente)|" +
                "esta\\s+(mare|égua|egua|paciente)|a\\s+égua|a\\s+egua|o\\s+cavalo)\\b",
        )
    private val recordActionRegex =
        Regex(
            "\\b(given|received|recorded|logged|treated|had|needs?|shows?|has|have|" +
                "recebeu|receberam|registou|registado|registada|teve|tinham?|precisa|" +
                "precisam|mostra|mostram|administrad[oa]|realizou|fez|aconteceu|" +
                "ocorreu|foi)\\b",
        )

    private val patientNameStopWords =
        setOf(
            "what",
            "when",
            "which",
            "who",
            "how",
            "why",
            "where",
            "did",
            "do",
            "does",
            "is",
            "are",
            "was",
            "were",
            "can",
            "could",
            "would",
            "should",
            "the",
            "a",
            "an",
            "of",
            "for",
            "to",
            "in",
            "on",
            "at",
            "and",
            "or",
            "any",
            "have",
            "has",
            "had",
            "my",
            "our",
            "your",
            "this",
            "that",
            "these",
            "those",
            "tell",
            "me",
            "about",
            "please",
            "patient",
            "patients",
            "horse",
            "horses",
            "mare",
            "mares",
            "cavalo",
            "cavalos",
            "égua",
            "éguas",
            "paciente",
            "pacientes",
            "o",
            "os",
            "as",
            "um",
            "uma",
            "uns",
            "umas",
            "que",
            "foi",
            "são",
            "sao",
            "não",
            "nao",
            "há",
            "ha",
            "do",
            "da",
            "dos",
            "das",
            "em",
            "com",
            "para",
            "por",
            "como",
            "porque",
            "porquê",
            "tenho",
            "temos",
            "está",
            "esta",
            "é",
            "e",
            "aconteceu",
            "ocorreu",
            "pregnant",
            "pregnancy",
            "gestation",
            "vaccination",
            "vaccinations",
            "vaccine",
            "booster",
            "farrier",
            "visit",
            "visits",
            "deworming",
            "weight",
            "ultrasound",
            "latest",
            "last",
            "previous",
            "recent",
            "record",
            "records",
            "treatment",
            "treatments",
            "received",
            "given",
            "happened",
            "occurred",
            "activity",
            "month",
            "week",
            "year",
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
            "quando",
            "qual",
            "quais",
            "quantos",
            "quantas",
            "último",
            "última",
            "ultimo",
            "ultima",
            "recente",
            "recentes",
            "registo",
            "registos",
        )

    private val questionStartWords =
        setOf("what", "when", "which", "who", "how", "why", "where", "qual", "quais", "quantos", "quantas")

    fun isRecordQuestion(
        query: String,
        scopedPatientName: String?,
        dateRange: RagDateRange?,
        patientNameMentioned: Boolean = false,
    ): Boolean {
        val lowered = query.lowercase()
        val hasRecordType = RecordTypeIntent.expectedRecordTypes(query).isNotEmpty()
        val hasPatientReference = patientPronounRegex.containsMatchIn(lowered)
        val hasRecordAction = recordActionRegex.containsMatchIn(lowered)
        return scopedPatientName != null ||
            patientNameMentioned ||
            hasPatientReference ||
            directRecordReferenceRegex.containsMatchIn(lowered) ||
            (dateRange != null && RecentActivityIntent.matches(query, dateRange)) ||
            hasRecordType &&
            (hasPatientReference || hasRecordAction)
    }

    /** True for an individual horse/patient reference without requiring its name. */
    fun hasIndividualPatientReference(query: String): Boolean = patientPronounRegex.containsMatchIn(query.lowercase())

    /** True when title-cased query text likely names a patient not in the active list. */
    fun hasLikelyNamedPatientReference(query: String): Boolean =
        query
            .split(Regex("\\s+"))
            .mapIndexed { index, token -> index to cleanNameCandidate(token) }
            .any { (index, token) ->
                val lowered = token.lowercase()
                token.length >= 2 &&
                    token.first().isUpperCase() &&
                    lowered !in patientNameStopWords &&
                    (index > 0 || lowered !in questionStartWords)
            }

    private fun cleanNameCandidate(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';', '\'')
            .removeSuffix("'s")
            .removeSuffix("'S")
            .removeSuffix("’s")
            .removeSuffix("’S")
}
