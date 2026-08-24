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
     * System prompt for the veterinary records assistant. Kept under ~200
     * tokens: the RAG context budget is 4096 tokens total and
     * [RagConfig.systemReserveTokens] reserves this prompt's share.
     *
     * Grounding hardening for small on-device models: species identity,
     * context-only answering, citation and formatting rules are stated as
     * uppercase directives because small models weight them more reliably
     * than prose. The honest not-found line is localized via [strings] so a
     * PT device is told to answer with the PT sentence.
     */
    fun systemPrompt(strings: AssistantStrings = EnAssistantStrings): String =
        """
        YOU ARE THE RECORDS ASSISTANT FOR AN EQUINE (HORSE) VETERINARY CLINIC. ALL PATIENTS ARE HORSES.
        ANSWER ONLY FROM THE CONTEXT BELOW. DO NOT USE OUTSIDE KNOWLEDGE. IF THE CONTEXT DOES NOT CONTAIN THE ANSWER, SAY EXACTLY: ${strings.notFoundInRecords}
        ALWAYS CITE YOUR SOURCES: WHEN THE CONTEXT CONTAINS RECORDS, YOUR ANSWER MUST INCLUDE AT LEAST ONE BRACKETED HEADER FROM THE CONTEXT VERBATIM, e.g. [Vaccination #123] Thunder.
        NEVER invent sources, citations, or URLs. Cite only bracketed headers present in the context verbatim, e.g. [Vaccination #123] Thunder.
        NEVER repeat context blocks, separators like ---, or the Question line. Answer in your own words.
        You MAY combine facts from multiple provided records.
        WRITE PLAIN TEXT ONLY: no markdown, no bold (**), no links, no bullet symbols other than dashes.
        Lead with the direct answer, then details as short labeled lines using dashes.
        Never invent treatments, dosages, or dates.
        Sound human: warm, natural sentences like a trusted colleague talking
        to the vet. Contractions welcome. A brief friendly opener is fine when
        it fits. Still concise - no filler, no apologies unless warranted.
        """.trimIndent()

    /** Back-compat alias over [systemPrompt] with English strings. */
    val SYSTEM_PROMPT: String = systemPrompt()

    private fun clean(token: String): String = token.trim('?', ',', '.', '!', ':', ';')

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
     * "thunder* OR farrier*". Unlike [toOrQuery], the output is already a
     * MATCH expression - it must NOT be routed through SearchUseCase, whose
     * tokenizer stars every whitespace token and would corrupt the operators
     * ("OR" becomes "OR*", a syntax error). Callers pass it straight to the
     * repository. Returns the empty string when no content token survives.
     */
    fun toFtsOrQuery(query: String): String = contentTokens(query).joinToString(" OR ") { "$it*" }

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
