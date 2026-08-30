package com.github.rodrigotimoteo.animally.llm

/**
 * Separates questions about the user's data from general veterinary
 * education. Cloud models may answer the latter, but never get to use cloud
 * flexibility as an excuse to fill a missing record with a guess.
 */
internal object RecordQuestionIntent {
    private data class RecordQuestionSignals(
        val scopedPatientName: String?,
        val patientNameMentioned: Boolean,
        val hasPatientReference: Boolean,
        val hasRecordType: Boolean,
        val hasRecordAction: Boolean,
        val hasNamedRecordCue: Boolean,
        val hasNamedPatientReference: Boolean,
        val hasPatientSubjectCue: Boolean,
        val hasDirectReference: Boolean,
        val hasRecentActivity: Boolean,
        val hasCorpusReference: Boolean,
    ) {
        val hasDirectScope: Boolean
            get() =
                scopedPatientName != null ||
                    patientNameMentioned ||
                    hasPatientReference ||
                    hasNamedPatientReference ||
                    hasDirectReference ||
                    hasRecentActivity

        val hasTypedScope: Boolean
            get() =
                hasRecordType &&
                    (
                        hasPatientReference ||
                            hasNamedPatientReference ||
                            hasRecordAction ||
                            hasNamedRecordCue ||
                            hasPatientSubjectCue ||
                            hasCorpusReference
                    ) ||
                    hasNamedRecordCue &&
                    hasCorpusReference
    }

    private val directRecordReferenceRegex =
        Regex(
            "\\b(my|our|your|active|current)\\s+(records?|patients?|horses|history|timeline|data|dataset)\\b|" +
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
    private val namedPatientReferenceRegex =
        Regex(
            "\\b(?:horse|horses|mare|mares|patient|patients|cavalo|cavalos|" +
                "égua|éguas|egua|eguas|paciente|pacientes)\\s+" +
                "(?:named|called|chamado|chamada|de\\s+nome)\\s+" +
                "[\\p{L}][\\p{L}\\p{N}_-]{1,}",
            RegexOption.IGNORE_CASE,
        )

    /** Singular references used to scope records to one patient. */
    private val individualPronounRegex =
        Regex(
            "\\b(she|he|her|his|ela|ele|dela|dele|" +
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
    private val educationalQuestionRegex =
        Regex(
            "^(?:what\\s+(?:is|are)|define|(?:can|could)\\s+you\\s+explain|explain|" +
                "(?:can|could)\\s+you\\s+tell\\s+me\\s+about|tell\\s+me\\s+about|" +
                "how\\s+(?:does|do)\\s+.+\\b(?:work|affect|develop|happen)\\b|" +
                "what\\s+causes|why\\s+(?:does|do|is|are)|" +
                "o\\s+que\\s+(?:é|e|são|sao)|(?:podes?|poderia)\\s+explicar|" +
                "(?:podes?|poderia)\\s+falar[- ]me\\s+sobre|fala[- ]me\\s+sobre|" +
                "como\\s+(?:funciona|funcionam)|o\\s+que\\s+causa|" +
                "porque\\s+(?:é|e)|explica|explique)" +
                "(?=$|[^\\p{L}\\p{N}_])",
            RegexOption.IGNORE_CASE,
        )
    private val generalKnowledgeQuestionRegex =
        Regex(
            "^(?:who\\s+(?:wrote|invented|discovered|directed|painted|founded|created|composed|" +
                "designed|played)|where\\s+(?:is|are|was|were)|when\\s+did\\s+.+\\b(?:happen|occur|begin|end)\\b|" +
                "how\\s+do\\s+i\\s+(?:care\\s+for|feed|look\\s+after|help|manage|train)\\s+" +
                "(?:a|an|the)?\\s*(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|egua|égua|éguas|" +
                "eguas)|what\\s+should\\s+i\\s+(?:feed|know|do)\\s+(?:for|with)\\s+" +
                "(?:a|an|the)?\\s*(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|egua|égua|éguas|eguas)|" +
                "can\\s+(?:a|an|the)?\\s*(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|" +
                "egua|égua|éguas|eguas)\\s+" +
                "(?:eat|drink|have|be|sleep|run)|como\\s+posso\\s+(?:cuidar|alimentar|ajudar)\\s+" +
                "(?:um|uma|o|a)?\\s*(?:cavalo|cavalos|égua|éguas|potro|potros))(?=$|[^\\p{L}\\p{N}_])",
            RegexOption.IGNORE_CASE,
        )

    /** Common husbandry questions should reach a configured cloud model, even when they say "my horse". */
    private val genericHorseCareQuestionRegex =
        Regex(
            "^(?:what\\s+(?:should|can|could)\\s+i\\s+(?:feed|give|do|know)\\s+(?:for\\s+)?" +
                "(?:my|our|your|a|an|the)?\\s*(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|" +
                "égua|éguas|egua|eguas)|" +
                "what\\s+(?:is|are)\\s+(?:a\\s+)?(?:good|healthy|suitable|best)\\s+" +
                "(?:diet|food|feed|nutrition)\\s+for\\s+(?:my|our|your|a|an|the)?\\s*" +
                "(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|égua|éguas|egua|eguas)|" +
                "how\\s+(?:should|can|do|could)\\s+(?:i|we)\\s+(?:care\\s+for|look\\s+after|" +
                "manage|feed|help|train)\\s+(?:my|our|your|a|an|the)?\\s*" +
                "(?:horse|horses|mare|mares|foal|foals|cavalo|cavalos|égua|éguas|egua|eguas)|" +
                "can\\s+(?:my|our|your|a|an|the)?\\s*(?:horse|horses|mare|mares|foal|foals|" +
                "cavalo|cavalos|égua|éguas|egua|eguas)\\s+(?:eat|drink|have|be|sleep|run)|" +
                "como\\s+(?:devo|posso|podemos|pode)\\s+(?:cuidar|alimentar|ajudar|tratar)\\s+" +
                "(?:(?:(?:o|a)\\s+)?(?:meu|minha|meus|minhas|um|uma)\\s+)?" +
                "(?:cavalo|cavalos|égua|éguas|egua|eguas)\\b)(?=$|[^\\p{L}\\p{N}])",
            RegexOption.IGNORE_CASE,
        )
    private val recordCorpusReferenceRegex =
        Regex(
            "\\b(records?|notes?|entries?|cards?|files?|timeline|dataset|" +
                "registos?|notas?|entradas?|ficha|linha\\s+do\\s+tempo|" +
                "aplicação|aplicacao)\\b|" +
                "\\b(?:my|our|your|patient|horse|mare|clinical|medical)\\s+(?:history|data)\\b|" +
                "\\b(?:meu|minha|meus|minhas|nosso|nossa|paciente|cavalo|égua|egua|clínico|clinico|" +
                "histórico|historico)\\s+(?:histórico|historico|dados|data)\\b|" +
                "\\b(?:history|histórico|historico|data|dados)\\s+(?:of|for|from|in|do|da|dos|das|" +
                "no|na|nos|nas)\\s+(?:my|our|your|patient|horse|mare|meu|minha|meus|minhas|" +
                "nosso|nossa|paciente|cavalo|cavalos|égua|éguas|egua|eguas)\\b|" +
                "\\b[\\p{L}][\\p{L}\\p{N}_-]{1,}['’]s\\s+(?:history|data|histórico|historico)\\b",
        )
    private val namedPatientPossessiveRegex =
        Regex("\\b[\\p{L}][\\p{L}\\p{N}_-]{1,}['’]s\\b")
    private val gestationPopulationReferenceRegex =
        Regex(
            "\\b(which|what|how many|are any|are there|do any)\\s+(?:of\\s+)?" +
                "(?:my|our|your|the)?\\s*(mares?|horses?|patients?)\\b|" +
                "\\b(quais|que|quantas|quantos|há|ha|existem)\\s+(?:das?\\s+|dos?\\s+)?" +
                "(?:minhas|nossas|vossas|meus|nossos|as|os)?\\s*(éguas?|cavalos?|pacientes?)\\b|" +
                "\\b(my|our|your|the)\\s+(mares?|horses?|patients?)\\b|" +
                "\\b(as\\s+minhas|as\\s+nossas|as\\s+vossas|os\\s+meus|os\\s+nossos)\\s+" +
                "(éguas?|cavalos?|pacientes?)\\b",
        )
    private val gestationStatusReferenceRegex =
        Regex(
            "\\b(pregnant|pregnancy|pregnancies|gestation|gestations|in\\s+foal|foaling|" +
                "prenha|prenhe|prenhes|prenhez|gestação|gestacoes|gestações|parição|parições)\\b",
        )
    private val patientSubjectRecordCueRegex =
        Regex(
            "\\b(?:is|are|was|were|está|esta|estão|estao)\\s+(?:the|a|o)?\\s*" +
                "(?!she\\b|he\\b|her\\b|his\\b|they\\b|their\\b|ela\\b|ele\\b|dela\\b|dele\\b|" +
                "currently\\b|current\\b|now\\b|already\\b|still\\b|equine\\b|" +
                "horse\\b|horses\\b|mare\\b|mares\\b|foal\\b|foals\\b|patient\\b|patients\\b|" +
                "cavalo\\b|cavalos\\b|égua\\b|éguas\\b|egua\\b|eguas\\b|paciente\\b|pacientes\\b)" +
                "[\\p{L}][\\p{L}\\p{N}_-]{1,}\\s+" +
                "(?:pregnant|pregnancy|gestation|in\\s+foal|prenha|prenhe|prenhez|gestação|gestacao)\\b|" +
                "\\b(?:did|receiv(?:e|ed)|received|teve|recebeu|receberam)\\s+" +
                "(?:the|a|o)?\\s*" +
                "(?!she\\b|he\\b|her\\b|his\\b|they\\b|their\\b|ela\\b|ele\\b|dela\\b|dele\\b|" +
                "currently\\b|current\\b|now\\b|already\\b|still\\b|equine\\b|" +
                "horse\\b|horses\\b|mare\\b|mares\\b|foal\\b|foals\\b|patient\\b|patients\\b|" +
                "cavalo\\b|cavalos\\b|égua\\b|éguas\\b|egua\\b|eguas\\b|paciente\\b|pacientes\\b)" +
                "[\\p{L}][\\p{L}\\p{N}_-]{1,}\\b",
            RegexOption.IGNORE_CASE,
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
            "explain",
            "analyse",
            "analyze",
            "compare",
            "show",
            "list",
            "give",
            "summarize",
            "summarise",
            "calculate",
            "compute",
            "find",
            "describe",
            "faz",
            "fazer",
            "analisa",
            "analise",
            "compara",
            "mostra",
            "lista",
            "podes",
            "poderia",
            "fala",
            "fala-me",
            "explique",
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
            "current",
            "currently",
            "now",
            "today",
            "date",
            "day",
            "days",
            "along",
            "due",
            "active",
            "positive",
            "negative",
            "viable",
            "status",
            "este",
            "neste",
            "nesta",
            "atualmente",
            "agora",
            "dia",
            "dias",
            "estão",
            "estao",
            "prenha",
            "prenhe",
            "prenhes",
            "prenhez",
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
            "receive",
            "received",
            "receives",
            "care",
            "feed",
            "give",
            "given",
            "metronidazole",
            "antibiotic",
            "antibiotics",
            "medication",
            "medications",
            "medicine",
            "medicines",
            "drug",
            "drugs",
            "prescription",
            "prescriptions",
            "dose",
            "doses",
            "dosage",
            "dosages",
            "história",
            "historia",
            "histórico",
            "historico",
        )

    private val questionStartWords =
        setOf(
            "what",
            "when",
            "which",
            "who",
            "how",
            "why",
            "where",
            "qual",
            "quais",
            "quantos",
            "quantas",
            "explain",
            "analyse",
            "analyze",
            "compare",
            "show",
            "list",
            "give",
            "summarize",
            "summarise",
            "calculate",
            "compute",
            "find",
            "describe",
            "faz",
            "fazer",
            "analisa",
            "analise",
            "compara",
            "mostra",
            "lista",
        )

    fun isRecordQuestion(
        query: String,
        scopedPatientName: String?,
        dateRange: RagDateRange?,
        patientNameMentioned: Boolean = false,
    ): Boolean {
        if (isGeneralKnowledgeQuestion(query) && scopedPatientName == null && !patientNameMentioned) return false
        val lowered = query.lowercase()
        val hasRecordType = RecordTypeIntent.expectedRecordTypes(query).isNotEmpty()
        val hasPatientReference = patientPronounRegex.containsMatchIn(lowered)
        val hasRecordAction = recordActionRegex.containsMatchIn(lowered)
        val hasNamedRecordCue = hasNamedPatientRecordCue(query)
        val hasPatientSubjectCue = hasPatientSubjectRecordCue(query)
        val signals =
            RecordQuestionSignals(
                scopedPatientName = scopedPatientName,
                patientNameMentioned = patientNameMentioned,
                hasPatientReference = hasPatientReference,
                hasRecordType = hasRecordType,
                hasRecordAction = hasRecordAction,
                hasNamedRecordCue = hasNamedRecordCue,
                hasNamedPatientReference = namedPatientReferenceRegex.containsMatchIn(lowered),
                hasPatientSubjectCue = hasPatientSubjectCue,
                hasDirectReference = directRecordReferenceRegex.containsMatchIn(lowered),
                hasRecentActivity = dateRange != null && RecentActivityIntent.matches(query, dateRange),
                hasCorpusReference = recordCorpusReferenceRegex.containsMatchIn(lowered),
            )
        return signals.hasDirectScope ||
            signals.hasTypedScope ||
            hasGestationPopulationReference(query)
    }

    /**
     * True for a singular horse/patient reference without requiring its name.
     *
     * They/their remain record-question signals, but are deliberately not
     * treated as a one-patient scope: in questions such as "which mares ...
     * are they due?" they refer to a group, and narrowing to an arbitrary
     * individual would hide authoritative records.
     */
    fun hasIndividualPatientReference(query: String): Boolean = individualPronounRegex.find(query.lowercase()) != null

    /** True for a population question that explicitly asks about pregnancy records. */
    fun hasGestationPopulationReference(query: String): Boolean {
        val lowered = query.lowercase()
        return gestationPopulationReferenceRegex.containsMatchIn(lowered) &&
            gestationStatusReferenceRegex.containsMatchIn(lowered)
    }

    /** True for definition/explanation prompts whose title case is usually a clinical term, not a patient name. */
    fun isEducationalQuestion(query: String): Boolean = educationalQuestionRegex.containsMatchIn(query.trim())

    /**
     * True when an educational-looking question is about general knowledge,
     * rather than an Animally record. A known patient scope still wins in the
     * caller through [isRecordQuestion]; this helper is for deciding whether
     * incidental database hits should be kept out of a cloud answer.
     */
    fun isGeneralKnowledgeQuestion(query: String): Boolean {
        val genericHorseCare = genericHorseCareQuestionRegex.containsMatchIn(query.trim())
        if (!isEducationalQuestion(query) &&
            !generalKnowledgeQuestionRegex.containsMatchIn(query.trim()) &&
            !genericHorseCare
        ) {
            return false
        }
        val lowered = query.lowercase()
        val hasRecordSpecificCue =
            namedPatientReferenceRegex.containsMatchIn(lowered) ||
                directRecordReferenceRegex.containsMatchIn(lowered) ||
                recordCorpusReferenceRegex.containsMatchIn(lowered) ||
                hasNamedPatientRecordCue(query) ||
                hasGestationPopulationReference(query)
        return !hasRecordSpecificCue &&
            (genericHorseCare || !patientPronounRegex.containsMatchIn(lowered))
    }

    /** True when title-cased query text likely names a patient not in the active list. */
    fun hasLikelyNamedPatientReference(query: String): Boolean {
        if (isGeneralKnowledgeQuestion(query)) return false
        val lowered = query.lowercase()
        if (!hasPatientRecordCue(query, lowered)) return false
        val hasTitleCasedCandidate =
            query
                .split(Regex("\\s+"))
                .mapIndexed { index, token -> index to cleanNameCandidate(token) }
                .any { (index, token) ->
                    val candidate = token.lowercase()
                    token.length >= 2 &&
                        token.first().isUpperCase() &&
                        candidate !in patientNameStopWords &&
                        (index > 0 || candidate !in questionStartWords)
                }
        return namedPatientReferenceRegex.containsMatchIn(lowered) ||
            hasTitleCasedCandidate ||
            patientSubjectRecordCueRegex.containsMatchIn(lowered)
    }

    private fun hasPatientRecordCue(
        query: String,
        lowered: String,
    ): Boolean =
        RecordTypeIntent.expectedRecordTypes(query).isNotEmpty() ||
            directRecordReferenceRegex.containsMatchIn(lowered) ||
            patientPronounRegex.containsMatchIn(lowered) ||
            namedPatientReferenceRegex.containsMatchIn(lowered) ||
            recordCorpusReferenceRegex.containsMatchIn(lowered) ||
            hasNamedPatientRecordCue(query) ||
            patientSubjectRecordCueRegex.containsMatchIn(lowered)

    /** Title-cased patient-like text paired with an explicit record signal. */
    private fun hasNamedPatientRecordCue(query: String): Boolean {
        if (isEducationalQuestion(query) && !namedPatientPossessiveRegex.containsMatchIn(query)) return false
        val lowered = query.lowercase()
        val hasTitleCasedCandidate =
            query
                .split(Regex("\\s+"))
                .mapIndexed { index, token -> index to cleanNameCandidate(token) }
                .any { (index, token) ->
                    val candidate = token.lowercase()
                    token.length >= 2 &&
                        token.first().isUpperCase() &&
                        candidate !in patientNameStopWords &&
                        (index > 0 || candidate !in questionStartWords)
                }
        if (!hasTitleCasedCandidate) return false
        return namedPatientPossessiveRegex.containsMatchIn(query) ||
            RecordTypeIntent.expectedRecordTypes(query).isNotEmpty() ||
            recordActionRegex.containsMatchIn(lowered)
    }

    private fun hasPatientSubjectRecordCue(query: String): Boolean =
        patientSubjectRecordCueRegex.containsMatchIn(
            query.lowercase(),
        )

    private fun cleanNameCandidate(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';', '\'')
            .removeSuffix("'s")
            .removeSuffix("'S")
            .removeSuffix("’s")
            .removeSuffix("’S")
}
