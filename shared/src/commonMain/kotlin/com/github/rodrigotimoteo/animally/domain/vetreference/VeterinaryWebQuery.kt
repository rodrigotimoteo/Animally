package com.github.rodrigotimoteo.animally.domain.vetreference

import com.github.rodrigotimoteo.animally.domain.vetreference.generated.NlmMedicalVocabulary
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource

/**
 * Privacy and scope gate for public veterinary-reference searches.
 *
 * The provider never receives the raw user question. Only terms from the
 * curated vocabulary and generated NLM MeSH index are forwarded, which keeps
 * patient names, owner details, record text, and identifiers out of the
 * external request.
 */
object VeterinaryWebQuery {
    private val unsafeIdentifierRegex =
        Regex(
            "\\d|\\b(?:my|our|your|this|that|she|he|her|his|their|" +
                "meu|minha|minhas|meus|nosso|nossa|dela|dele|ela|ele)\\b|" +
                "\\b[\\p{L}][\\p{L}\\p{N}_-]{1,}['’]s\\b",
            RegexOption.IGNORE_CASE,
        )

    /** Species, qualifiers, and app-specific language terms not supplied by MeSH. */
    private val curatedTopicTerms =
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
            // Common user spelling; normalize it before the public lookup.
            "laminites",
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
            "inflammation",
            "inflammatory",
            "laminae",
            "hoof",
            "coffin",
            "bone",
            "arthritis",
            "osteoarthritis",
            "dermatitis",
            "abscess",
            "edema",
            "oedema",
            "swelling",
            "fever",
            "pain",
            "analgesic",
            "antiinflammatory",
            "antibiotic",
            "antimicrobial",
            "ibuprofen",
            "phenylbutazone",
            "flunixin",
            "firocoxib",
            "medication",
            "medicine",
            "drug",
            "dose",
            "dosage",
            "adverse",
            "prognosis",
            "disease",
            "disorder",
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
            "strangles",
            "herpesvirus",
            "coggins",
            "anemia",
            "anaemia",
            "anaphylaxis",
            "shock",
            "sepsis",
            "colitis",
            "impaction",
            "choke",
            "dental",
            "tooth",
            "allergy",
            "allergies",
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
            "inflamação",
            "inflamacao",
            "artrite",
            "artrose",
            "casco",
            "osso",
            "infeção",
            "infecao",
            "abcesso",
            "medicamento",
            "medicação",
            "medicacao",
            "doença",
            "doenca",
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

    private val topicTerms = curatedTopicTerms + NlmMedicalVocabulary.terms
    private val fuzzyTopicTermsByLength = topicTerms.groupBy(String::length)

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
            "laminites" to "laminitis",
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
            "inflamação" to "inflammation",
            "inflamacao" to "inflammation",
            "artrite" to "arthritis",
            "artrose" to "osteoarthritis",
            "casco" to "hoof",
            "osso" to "bone",
            "infeção" to "infection",
            "infecao" to "infection",
            "abcesso" to "abscess",
            "medicamento" to "medication",
            "medicação" to "medication",
            "medicacao" to "medication",
            "doença" to "disease",
            "doenca" to "disease",
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
            "leishmaniose" to "leishmaniasis",
            "leishmaniosis" to "leishmaniasis",
            "leishmania" to "leishmaniasis",
        )

    private val genericReferenceTerms =
        setOf("horse", "horses", "equine", "mare", "mares", "foal", "foals", "veterinary")

    /**
     * Search modifiers describe the requested angle, but they are not
     * independent subjects. Requiring every modifier in a short title or
     * abstract caused relevant disease articles to disappear for questions
     * such as "signs and causes of laminitis".
     */
    private val referenceQualifierTerms =
        setOf(
            "diagnosis",
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
            "toxic",
            "toxicity",
        )

    private val referenceSearchSynonyms =
        mapOf(
            "ultrasound" to setOf("ultrasound", "ultrasonography", "sonography"),
            "transrectal" to setOf("transrectal"),
            "vaccination" to setOf("vaccination", "vaccinated", "vaccine", "vaccines"),
            "deworming" to setOf("deworming", "dewormed", "parasite", "parasites"),
            "leishmaniasis" to setOf("leishmaniasis", "leishmania", "leishmaniosis", "leishmaniose"),
            "laminitis" to setOf("laminitis", "founder"),
            "lameness" to setOf("lameness", "claudication"),
            "symptoms" to setOf("symptoms", "symptom", "signs"),
            "signs" to setOf("signs", "symptoms", "symptom"),
            "causes" to setOf("causes", "cause", "etiology"),
            "cause" to setOf("causes", "cause", "etiology"),
            "treatment" to setOf("treatment", "treatments", "therapy", "management"),
            "management" to setOf("management", "treatment", "therapy"),
            "prevention" to setOf("prevention", "preventive", "prophylaxis"),
            "ibuprofen" to setOf("ibuprofen", "nsaid", "nsaids", "analgesic", "inflammatory"),
            "arthritis" to setOf("arthritis", "osteoarthritis", "arthrosis"),
            "laminae" to setOf("laminae", "lamina", "hoof"),
            "inflammation" to setOf("inflammation", "inflammatory"),
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
        val topicTerms =
            extractTopic(query)
                ?.split(' ')
                ?.filter { it !in genericReferenceTerms }
                .orEmpty()
        if (topicTerms.isEmpty()) return emptyList()
        // Keep every core subject strict. If a query only contains modifiers,
        // require those modifiers instead of allowing any medical article.
        val requiredTerms =
            topicTerms
                .filterNot { it in referenceQualifierTerms }
                .ifEmpty { topicTerms }
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
                .mapNotNull(::canonicalTopicToken)
                .distinct()
                .take(MAX_TOPIC_TERMS)
        return tokens.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private const val MAX_QUERY_CHARS = 240
    private const val MAX_TOPIC_TERMS = 8

    /**
     * Resolves exact vocabulary entries first, then accepts only a unique
     * nearby vocabulary term. This catches common speech-to-text errors while
     * keeping arbitrary words out of the public lookup query.
     */
    private fun canonicalTopicToken(token: String): String? {
        val exactMatch =
            referenceAliases[token]
                ?: NlmMedicalVocabulary.aliases[token]
                ?: token.takeIf { it in topicTerms }
        return exactMatch ?: fuzzyTopicToken(token)
    }

    private fun fuzzyTopicToken(token: String): String? {
        val normalizedToken = foldDiacritics(token)
        if (normalizedToken.length < MIN_FUZZY_TOKEN_LENGTH) return null
        val maxDistance = if (normalizedToken.length >= LONG_FUZZY_TOKEN_LENGTH) 2 else 1
        val candidates =
            (normalizedToken.length - maxDistance..normalizedToken.length + maxDistance)
                .flatMap { fuzzyTopicTermsByLength[it].orEmpty() }
        val distances =
            candidates.map { candidate ->
                FuzzyTopicMatch(
                    term = candidate,
                    distance = editDistance(normalizedToken, foldDiacritics(candidate)),
                )
            }
        val matches = distances.filter { it.distance <= maxDistance }.sortedBy { it.distance }
        val best = matches.firstOrNull()
        val isUnique = best != null && matches.count { it.distance == best.distance } == 1
        return best?.takeIf { isUnique }?.let {
            referenceAliases[it.term] ?: NlmMedicalVocabulary.aliases[it.term] ?: it.term
        }
    }

    private fun foldDiacritics(value: String): String =
        value
            .map { character ->
                when (character) {
                    'á', 'à', 'â', 'ã', 'ä' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'í', 'ì', 'î', 'ï' -> 'i'
                    'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
                    'ú', 'ù', 'û', 'ü' -> 'u'
                    'ç' -> 'c'
                    'ñ' -> 'n'
                    else -> character
                }
            }.joinToString("")

    /** Levenshtein distance; the vocabulary is small and this stays allocation-light. */
    private fun editDistance(
        left: String,
        right: String,
    ): Int =
        when {
            left == right -> 0
            left.isEmpty() -> right.length
            right.isEmpty() -> left.length
            else -> {
                var previous = IntArray(right.length + 1) { it }
                for (leftIndex in left.indices) {
                    val current = IntArray(right.length + 1)
                    current[0] = leftIndex + 1
                    for (rightIndex in right.indices) {
                        current[rightIndex + 1] =
                            minOf(
                                current[rightIndex] + 1,
                                previous[rightIndex + 1] + 1,
                                previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1,
                            )
                    }
                    previous = current
                }
                previous[right.length]
            }
        }

    private data class FuzzyTopicMatch(
        val term: String,
        val distance: Int,
    )

    private const val MIN_FUZZY_TOKEN_LENGTH = 4
    private const val LONG_FUZZY_TOKEN_LENGTH = 8

    private fun tokenizeReferenceText(text: String): Set<String> =
        text
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .toSet()
}
