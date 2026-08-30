package com.github.rodrigotimoteo.animally.domain.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource

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
            "vacina",
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
            "ultrasound",
            "ultrasonography",
            "sonography",
            "ecografia",
            "ultrassom",
            "transrectal",
            "transretal",
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

    /** Maps supported Portuguese search terms to the English reference vocabulary. */
    private val referenceAliases =
        mapOf(
            "cavalo" to "horse",
            "cavalos" to "horses",
            "equino" to "equine",
            "equinos" to "equine",
            "veterinária" to "veterinary",
            "veterinaria" to "veterinary",
            "vacina" to "vaccine",
            "laminite" to "laminitis",
            "ecografia" to "ultrasound",
            "ultrassom" to "ultrasound",
            "transretal" to "transrectal",
            "cólica" to "colic",
            "colica" to "colic",
            "úlcera" to "ulcer",
            "úlceras" to "ulcer",
            "ulceras" to "ulcer",
            "claudicação" to "lameness",
            "claudicacao" to "lameness",
            "vacinação" to "vaccination",
            "vacinacao" to "vaccination",
            "gestação" to "gestation",
            "gestacao" to "gestation",
            "nutrição" to "nutrition",
            "nutricao" to "nutrition",
            "alimentação" to "nutrition",
            "alimentacao" to "nutrition",
            "tratamento" to "treatment",
            "prevenção" to "prevention",
            "prevencao" to "prevention",
            "sintomas" to "symptoms",
            "sinais" to "signs",
            "causas" to "causes",
        )

    private val genericReferenceTerms =
        setOf("horse", "horses", "equine", "mare", "mares", "foal", "foals", "veterinary")

    private val referenceSearchSynonyms =
        mapOf(
            "ultrasound" to setOf("ultrasound", "ultrasonography", "sonography"),
            "transrectal" to setOf("transrectal"),
            "vaccination" to setOf("vaccination", "vaccinated", "vaccine", "vaccines"),
            "deworming" to setOf("deworming", "dewormed", "parasite", "parasites"),
        )

    /** Returns true only for general medical/veterinary questions. */
    fun isMedicalQuestion(query: String): Boolean {
        val topic = extractTopic(query) ?: return false
        return topic.split(' ').any { it in medicalTopicTerms }
    }

    /**
     * Drops publisher results that do not mention every specific requested
     * topic. A reputable domain can still return a semantically unrelated
     * result, which must never be placed in a medical answer context.
     */
    fun filterRelevantSources(
        query: String,
        sources: List<VeterinaryWebSource>,
    ): List<VeterinaryWebSource> {
        val requiredTerms =
            extractTopic(query)
                ?.split(' ')
                ?.filter { it !in genericReferenceTerms }
                .orEmpty()
        if (requiredTerms.isEmpty()) return emptyList()
        return sources.filter { source ->
            val sourceTerms = tokenizeReferenceText("${source.title} ${source.excerpt}")
            requiredTerms.all { term ->
                (referenceSearchSynonyms[term] ?: setOf(term)).any(sourceTerms::contains)
            }
        }
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
                .map { referenceAliases[it] ?: it }
                .distinct()
                .take(MAX_TOPIC_TERMS)
        return tokens.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private const val MAX_QUERY_CHARS = 240
    private const val MAX_TOPIC_TERMS = 8

    private fun tokenizeReferenceText(text: String): Set<String> =
        text
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .toSet()
}
