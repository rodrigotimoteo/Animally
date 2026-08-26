package com.github.rodrigotimoteo.animally.llm

/**
 * Prompt text and query shaping for the on-device assistant.
 *
 * The system prompt establishes the assistant's role, scope, and citation
 * rules; [enrichQuery] strips conversational filler so the FTS query matches
 * record content instead of question words.
 */
object AssistantPrompts {
    private val FILLER_WORDS =
        setOf(
            "what",
            "when",
            "which",
            "who",
            "did",
            "do",
            "does",
            "how",
            "is",
            "are",
            "was",
            "were",
            "the",
            "a",
            "an",
            "of",
            "for",
            "to",
            "in",
            "on",
            "any",
            "have",
            "has",
            "had",
            "she",
            "he",
            "her",
            "his",
            "it",
            "there",
            "me",
            "my",
            "i",
            "tell",
            "about",
        )

    private val GREETINGS =
        setOf(
            "hi",
            "hello",
            "hey",
            "olá",
            "ola",
            "oi",
            "bom dia",
            "boa tarde",
            "boa noite",
        )

    // Portuguese markers used to mirror the user's language per question
    // (device locale stays the default; a PT question gets a PT turn even on
    // an EN device). Conservative: question words + common veterinary nouns;
    // single ambiguous tokens are excluded except "é".
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
            "vacinação",
            "tratamento",
            "tratamentos",
            "gestação",
            "peso",
            "registo",
            "registos",
            "é",
        )

    // Any PT diacritic strongly signals Portuguese in a Latin-script query.
    private val portugueseDiacriticRegex = Regex("[áâãàçéêíóôõú]")

    /**
     * Domain synonym groups for retrieval recall: when a query token matches
     * a member, the group's remaining members are appended as OR-terms so a
     * natural phrasing ("in foal", "shod") can still reach records indexed
     * under different vocabulary. Multi-word members are allowed; they are
     * emitted as quoted FTS phrases. At most [MAX_SYNONYM_GROUPS] groups are
     * expanded per query (first matches in declared order) to bound query
     * growth. Note: expansion lives in the FTS-shaped builders
     * ([toFtsOrQuery]/[toFtsAndQuery]) — NOT in [enrichQuery], whose output
     * is AND-joined token-by-token by SearchUseCase and would turn embedded
     * OR syntax into an invalid MATCH expression.
     */
    private val SYNONYM_GROUPS: List<List<String>> =
        listOf(
            listOf("pregnant", "in foal", "gestation", "foaling", "bred"),
            listOf("shod", "shoeing", "shoes", "trim", "farrier"),
            listOf("vaccination", "vaccine", "booster", "shot"),
            listOf("embryo transfer", "flush", "donor", "recipient"),
            listOf("colic", "abdominal pain"),
            // Morphology bridge, NOT stemming: "tendon*" cannot prefix-match
            // "tendinitis" (diverges at the 6th character), so the pair is
            // bridged lexically. FTS5 porter stemmer is unavailable here.
            listOf("tendon", "tendinitis"),
        )

    /** Maximum synonym groups expanded into a single query. */
    private const val MAX_SYNONYM_GROUPS = 2

    /**
     * System prompt for the veterinary records assistant. The default is kept
     * compact and deliberately strict for the roughly 4096-token on-device
     * Foundation Models path. Cloud fallback turns use a warmer policy that
     * permits general questions while keeping patient facts record-grounded.
     *
     * @param allowGeneralQuestions true when the router selected a cloud or
     *   tool-backed path for this turn.
     */
    fun systemPrompt(
        strings: AssistantStrings = EnAssistantStrings,
        allowGeneralQuestions: Boolean = false,
    ): String {
        val roleAndGrounding =
            if (allowGeneralQuestions) {
                """
                You are a warm, practical records assistant for an equine (horse) veterinary clinic. All patients in the records are horses.
                For questions about this user's records, use the context as the source of truth. Do not invent patient-specific facts. If a record question is not answered by the context, say exactly: ${strings.notFoundInRecords}
                For general, educational, or casual questions that are not asking for a patient record, you may use your general knowledge. Be clear when something is general rather than drawn from the records, and say when you are unsure.
                When you use a record from the context, cite its bracketed header verbatim at the end of the relevant sentence or line. Do not invent citations, sources, or URLs, and do not cite a record that does not support the sentence.
                [Summary] marks computed facts from the database: cite it when you use it, but never use it as a word in a sentence.
                Keep patient and owner facts separate, and never present general knowledge as a fact about a named patient.
                """.trimIndent()
            } else {
                """
                YOU ARE THE RECORDS ASSISTANT FOR AN EQUINE (HORSE) VETERINARY CLINIC. ALL PATIENTS ARE HORSES.
                ANSWER ONLY FROM THE CONTEXT BELOW. DO NOT USE OUTSIDE KNOWLEDGE. IF THE CONTEXT DOES NOT CONTAIN THE ANSWER, SAY EXACTLY: ${strings.notFoundInRecords}
                ALWAYS CITE YOUR SOURCES: WHEN THE CONTEXT CONTAINS RECORDS, YOUR ANSWER MUST INCLUDE AT LEAST ONE BRACKETED HEADER FROM THE CONTEXT VERBATIM. BRACKET FORMAT IS [RECORD_TYPE #ID] - FORMAT ONLY, NEVER A REAL CITATION OR A REAL RECORD NAME.
                PLACE EVERY CITATION AT THE END OF THE RELEVANT SENTENCE OR LINE. NEVER INSERT A CITATION BRACKET INSIDE A SENTENCE BETWEEN WORDS.
                NEVER INVENT DETAILS (BREEDS, DATES, COUNTS) THAT DO NOT APPEAR IN A HEADER OR RECORD LINE.
                [Summary] MARKS A COMPUTED-FACTS SOURCE: CITE IT WHEN USED, BUT NEVER USE IT AS A WORD IN A SENTENCE.
                STATE FACTS ABOUT THE SPECIFIC ENTITY THE USER NAMED - NEVER ATTRIBUTE OWNER-LEVEL FACTS TO A PATIENT OR PATIENT FACTS TO AN OWNER.
                DETERMINISTIC SUMMARY LINES ARE COMPUTED FACTS FROM THE DATABASE: TREAT THEM AS AUTHORITATIVE AND NEVER CONTRADICT THEM.
                NEVER invent sources, citations, or URLs. Cite only bracketed headers present in the context verbatim.
                """.trimIndent()
            }
        val commonGuidance =
            if (allowGeneralQuestions) {
                """
                Do not repeat context blocks, separators like ---, or the Question line. Answer in your own words.
                You may combine facts from multiple provided records when they are relevant.
                Write plain text only: no markdown, no bold (**), and no links. Keep citations as bracketed headers from the context only.
                For a simple question, answer naturally in one or two sentences. For several facts, use short paragraphs or a few dashes only when that genuinely makes the answer easier to scan.
                Never invent treatments, dosages, or patient-specific dates.
                Use contractions and a name naturally when it is relevant; do not force either one.
                Do not begin every answer with "According to the records" or "Based on the context".
                Avoid canned headings, robotic summaries, and unnecessary restatement of the question.
                A brief friendly opener is fine when it fits, but lead with the useful answer.
                Be concise without sounding abrupt; explain uncertainty plainly.
                """.trimIndent()
            } else {
                """
                NEVER repeat context blocks, separators like ---, or the Question line. Answer in your own words.
                You MAY combine facts from multiple provided records.
                WRITE PLAIN TEXT ONLY: no markdown, no bold (**), and no links. Keep citations as bracketed headers from the context only.
                For a simple question, answer naturally in one or two sentences. For several facts, use short paragraphs or a few dashes only when that genuinely makes the answer easier to scan.
                Never invent treatments, dosages, or dates.
                Sound human and warm, like a trusted colleague talking to the vet.
                Use contractions and a name naturally when it is relevant; do not force either one.
                Do not begin every answer with "According to the records" or "Based on the context".
                Avoid canned headings, robotic summaries, and unnecessary restatement of the question.
                A brief friendly opener is fine when it fits, but lead with the useful answer.
                Be concise without sounding abrupt; explain uncertainty plainly when the records are incomplete.
                """.trimIndent()
            }
        return listOf(roleAndGrounding, commonGuidance).joinToString("\n")
    }

    /** Back-compat alias over [systemPrompt] with English strings. */
    val SYSTEM_PROMPT: String = systemPrompt()

    /**
     * Normalizes one whitespace-delimited token for FTS matching: trailing
     * punctuation trimmed, INTERNAL apostrophes stripped ("Thunder's" ->
     * "Thunders"). The repository sanitizer splits tokens on non-alphanumeric
     * characters, so an apostrophe would emit a junk empty prefix plus a
     * stray "s*" term that pollutes the OR query and matches unrelated
     * records. Contraction words ("can't" -> "cant") degrade to harmless
     * misses.
     */
    private fun clean(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';')
            .replace("'", "")
            .replace("’", "")

    /**
     * Strips filler words from a user question for the FTS query only (the raw
     * question still goes to the LLM). Deterministic: tokenizes on whitespace,
     * lowercases each token for comparison after trimming trailing punctuation.
     *
     * @param query The raw user question.
     * @return The enriched query, or the original query when every token is filler.
     */
    fun enrichQuery(query: String): String {
        val kept = contentTokens(query)
        return if (kept.isEmpty()) query else kept.joinToString(" ")
    }

    /**
     * Friendly reply for greetings and small talk, or null when [query] is a
     * real question that should go through retrieval. Answering "hi" with the
     * no-results fallback reads as broken. The reply is localized via [strings].
     */
    fun greetingReply(
        query: String,
        strings: AssistantStrings = EnAssistantStrings,
    ): String? {
        val normalized = query.trim().lowercase().trim('!', '.', ',', '?', ' ')
        return if (normalized in GREETINGS) strings.greetingReply else null
    }

    /**
     * True when [query] reads as Portuguese: any PT marker token or any PT
     * diacritic. Used to mirror the user's language per question so a PT
     * question gets a PT reply even on an EN-locale device.
     */
    fun isPortugueseQuery(query: String): Boolean {
        if (portugueseDiacriticRegex.containsMatchIn(query)) return true
        return contentTokens(query).any { it.lowercase() in PORTUGUESE_MARKERS }
    }

    /**
     * FTS5-safe OR expression over the content (non-filler) tokens of [query]:
     * each token starred and joined with bare uppercase OR, e.g.
     * "thunder* OR farrier*". Synonym groups matched by the query's tokens
     * contribute their remaining members as extra OR-terms (at most
     * [MAX_SYNONYM_GROUPS] groups). Unlike [toOrQuery], the output is already
     * a MATCH expression - it must NOT be routed through SearchUseCase, whose
     * tokenizer stars every whitespace token and would corrupt the operators
     * ("OR" becomes "OR*", a syntax error). Callers pass it straight to the
     * repository. Returns the empty string when no content token survives.
     */
    fun toFtsOrQuery(query: String): String {
        val tokens = contentTokens(query)
        if (tokens.isEmpty()) return ""
        val terms = tokens.map { "$it*" } + synonymExpansionTerms(tokens)
        return terms.joinToString(" OR ")
    }

    /**
     * FTS5-safe AND expression over the content (non-filler) tokens of
     * [query]: each token starred and joined with bare uppercase AND, e.g.
     * "colic* AND surgery*". Mirrors SearchUseCase's tokenizer for callers
     * that bypass the use case and hand queries straight to the repository
     * (the RAG pipeline does this so both retrieval legs share one seam).
     * Returns the empty string when no content token survives.
     */
    fun toFtsAndQuery(query: String): String = contentTokens(query).joinToString(" AND ") { "$it*" }

    /**
     * OR-terms contributed by synonym expansion: for the first
     * [MAX_SYNONYM_GROUPS] groups containing at least one match against
     * [tokens] (single-word members match by token equality OR by
     * plural-folded equality — "vaccinations" reaches the singular-indexed
     * "vaccination" vocabulary; multi-word members match by phrase
     * containment in the lowercased raw query), every other member not
     * already present as a token is rendered as a starred term ("shoeing*")
     * or a starred quoted phrase ("in foal"*). The repository's sanitizer
     * passes these through unchanged.
     */
    private fun synonymExpansionTerms(tokens: List<String>): List<String> {
        val lowered = tokens.map(String::lowercase).toSet()
        val phrase = tokens.joinToString(" ").lowercase()
        val expansions = mutableListOf<String>()
        SYNONYM_GROUPS
            .filter { group -> group.matchesAny(lowered, phrase) }
            .take(MAX_SYNONYM_GROUPS)
            .forEach { group -> expansions += group.expansionTerms(lowered) }
        return expansions
    }

    /** Content tokens of [query]: cleaned, non-blank, non-filler. */
    private fun contentTokens(query: String): List<String> =
        query
            .split(Regex("\\s+"))
            .map(::clean)
            .filter { it.isNotBlank() && it.lowercase() !in FILLER_WORDS }

    /**
     * OR-joined variant of [query] over its content (non-filler) tokens.
     * FTS AND semantics zero out natural questions when any content word
     * misses ("which patients belong to Daniela"); one broad OR retry
     * recovers the matches. Returns the original query when nothing
     * survives cleaning so callers can fall through unchanged.
     */
    fun toOrQuery(query: String): String {
        val kept = contentTokens(query)
        return if (kept.isEmpty()) query else kept.joinToString(" OR ")
    }
}

/**
 * True when [this] synonym group is triggered by [loweredTokens] or [phrase].
 * Single-word members also match PLURAL query tokens via [singularize]
 * ("vaccinations" triggers the vaccination group) so plural questions reach
 * singular-indexed vocabulary through the emitted expansions.
 */
private fun List<String>.matchesAny(
    loweredTokens: Set<String>,
    phrase: String,
): Boolean =
    any { term ->
        if (' ' in term) {
            phrase.contains(term)
        } else {
            loweredTokens.any { token -> token == term || singularize(token) == term }
        }
    }

/** Starred terms for every member of [this] not already in [loweredTokens]. */
private fun List<String>.expansionTerms(loweredTokens: Set<String>): List<String> =
    mapNotNull { term ->
        when {
            ' ' in term -> "\"$term\"*" // starred quoted phrase keeps word order
            term in loweredTokens -> null
            else -> "$term*"
        }
    }

/** Minimum token length before plural suffixes are considered ("is" stays). */
private const val PLURAL_MIN_TOKEN_LENGTH = 4

/** Length of the stripped "ies" suffix ("vaccinations" keeps its own rule). */
private const val PLURAL_IES_SUFFIX_LENGTH = 3

/**
 * Naive English plural folder for synonym-group matching ONLY (never applied
 * to the FTS terms themselves): "vaccinations" -> "vaccination",
 * "vaccines" -> "vaccine". Conservative length guards keep short tokens
 * ("is", "gas") untouched.
 */
private fun singularize(token: String): String =
    when {
        token.length > PLURAL_MIN_TOKEN_LENGTH && token.endsWith("ies") ->
            token.dropLast(PLURAL_IES_SUFFIX_LENGTH) + "y"
        token.length > PLURAL_MIN_TOKEN_LENGTH && token.endsWith("s") ->
            token.dropLast(1)
        else -> token
    }
