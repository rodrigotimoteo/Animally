package com.github.rodrigotimoteo.animally.domain.vetreference

/**
 * Privacy and scope gate for public veterinary-reference searches.
 *
 * The provider never receives the raw user question. Only terms from the
 * controlled vocabulary below are forwarded, which keeps patient names,
 * owner details, record text, and identifiers out of the external request.
 */
object VeterinaryWebQuery {
    private val unsafeIdentifierRegex =
        Regex(
            "\\d|\\b(?:my|our|your|this|that|she|he|her|his|their|" +
                "meu|minha|minhas|meus|nosso|nossa|dela|dele|ela|ele)\\b|" +
                "\\b[\\p{L}][\\p{L}\\p{N}_-]{1,}['’]s\\b",
            RegexOption.IGNORE_CASE,
        )

    private val topicTerms =
        setOf(
            "horse",
            "horses",
            "equine",
            "veterinary",
            "mare",
            "mares",
            "foal",
            "foals",
            "cavalo",
            "cavalos",
            "equino",
            "equinos",
            "veterinária",
            "veterinaria",
            "laminitis",
            "laminite",
            "founder",
            "colic",
            "cólica",
            "colica",
            "ulcer",
            "ulcers",
            "úlcera",
            "úlceras",
            "ulceras",
            "lameness",
            "claudication",
            "claudicação",
            "claudicacao",
            "tendon",
            "tendinitis",
            "ligament",
            "fracture",
            "wound",
            "infection",
            "fever",
            "pain",
            "metabolic",
            "syndrome",
            "obesity",
            "nutrition",
            "diet",
            "pasture",
            "temperature",
            "vaccination",
            "vaccine",
            "vaccines",
            "vacinação",
            "vacinacao",
            "deworming",
            "parasite",
            "parasites",
            "parasitas",
            "gestation",
            "gestação",
            "gestacao",
            "foaling",
            "reproduction",
            "fertility",
            "endometritis",
            "mastitis",
            "pneumonia",
            "influenza",
            "tetanus",
            "rabies",
            "diagnosis",
            "diagnóstico",
            "diagnostico",
            "treatment",
            "management",
            "prevention",
            "symptoms",
            "signs",
            "causes",
            "cause",
            "acute",
            "chronic",
            "emergency",
            "first",
            "aid",
            "poisoning",
            "toxic",
            "toxicity",
            "dehydration",
            "diarrhea",
            "diarrhoea",
            "diarreia",
            "respiratory",
            "cough",
            "tosse",
            "nutrição",
            "nutricao",
            "alimentação",
            "alimentacao",
            "tratamento",
            "prevenção",
            "prevencao",
            "sintomas",
            "sinais",
            "causas",
            "emergência",
            "emergencia",
        )

    private val medicalTopicTerms =
        topicTerms -
            setOf(
                "horse",
                "horses",
                "equine",
                "veterinary",
                "mare",
                "mares",
                "foal",
                "foals",
                "cavalo",
                "cavalos",
                "equino",
                "equinos",
                "veterinária",
                "veterinaria",
            )

    /** Returns true only for general medical/veterinary questions. */
    fun isMedicalQuestion(query: String): Boolean {
        val topic = extractTopic(query) ?: return false
        return topic.split(' ').any { it in medicalTopicTerms }
    }

    /**
     * Builds a privacy-safe topic query. A null result means no public lookup
     * should be attempted.
     */
    fun extractTopic(query: String): String? {
        if (query.length > MAX_QUERY_CHARS || unsafeIdentifierRegex.containsMatchIn(query)) return null
        val tokens =
            query
                .lowercase()
                .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
                .split(Regex("\\s+"))
                .filter { it in topicTerms }
                .distinct()
                .take(MAX_TOPIC_TERMS)
        return tokens.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private const val MAX_QUERY_CHARS = 240
    private const val MAX_TOPIC_TERMS = 8
}
