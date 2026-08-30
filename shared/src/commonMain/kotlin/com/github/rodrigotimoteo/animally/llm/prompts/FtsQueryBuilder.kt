package com.github.rodrigotimoteo.animally.llm.prompts

import com.github.rodrigotimoteo.animally.llm.support.SharedStopWords

/**
 * FTS query shaping extracted from [com.github.rodrigotimoteo.animally.llm.AssistantPrompts].
 * Centralizes token cleaning and FTS expression building so both the RAG
 * pipeline and prior helpers share one implementation.
 */
internal object FtsQueryBuilder {
    /**
     * Normalizes one whitespace-delimited token for FTS matching: trailing
     * punctuation trimmed, internal apostrophes stripped ("Thunder's" ->
     * "Thunders"). Mirrors the former [AssistantPrompts.clean] exactly.
     */
    fun clean(token: String): String =
        token
            .trim('?', ',', '.', '!', ':', ';')
            .replace("'", "")
            .replace("’", "")

    /** Content tokens of [query]: cleaned, non-blank, non-filler. */
    fun contentTokens(query: String): List<String> =
        query
            .split(Regex("\\s+"))
            .map(::clean)
            .filter { it.isNotBlank() && it.lowercase() !in SharedStopWords.FILLER_WORDS }

    /**
     * Strips filler words from a user question for the FTS query only (the raw
     * question still goes to the LLM). Deterministic: tokenizes on whitespace,
     * lowercases each token for comparison after trimming trailing punctuation.
     */
    fun enrichQuery(query: String): String {
        val kept = contentTokens(query)
        return if (kept.isEmpty()) query else kept.joinToString(" ")
    }

    /**
     * FTS5-safe OR expression over the content (non-filler) tokens of [query].
     * Synonym groups matched by the query's tokens contribute their remaining
     * members as extra OR-terms (at most [SynonymExpander.MAX_SYNONYM_GROUPS] groups).
     */
    fun toFtsOrQuery(query: String): String {
        val tokens = contentTokens(query)
        if (tokens.isEmpty()) return ""
        val terms = tokens.map { "$it*" } + SynonymExpander.expansionTerms(tokens)
        return terms.joinToString(" OR ")
    }

    /**
     * FTS5-safe AND expression over the content (non-filler) tokens of [query].
     */
    fun toFtsAndQuery(query: String): String = contentTokens(query).joinToString(" AND ") { "$it*" }

    /**
     * OR-joined variant of [query] over its content (non-filler) tokens.
     * Returns the original query when nothing survives cleaning.
     */
    fun toOrQuery(query: String): String {
        val kept = contentTokens(query)
        return if (kept.isEmpty()) query else kept.joinToString(" OR ")
    }
}
