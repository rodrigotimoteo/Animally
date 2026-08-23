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
        )

    /**
     * System prompt for the veterinary records assistant. Kept under ~200
     * tokens: the RAG context budget is 4096 tokens total and
     * [RagConfig.systemReserveTokens] reserves this prompt's share.
     */
    val SYSTEM_PROMPT: String =
        """
        You are the on-device veterinary records assistant inside the Animally app,
        used by the treating veterinarian. Answer ONLY from the provided context.
        Rules:
        - Cite every fact inline using the bracketed source header from the
          context verbatim, e.g. [Vaccination #123] Thunder.
        - Lead with the direct answer, then details as short labeled bullets.
        - Bracketed headers in the context are source references; use them verbatim.
        - If the context lacks the answer, say what is missing. Never invent
          treatments, dosages, or dates.
        - If no context is provided, say no matching records were found and
          suggest what to search.
        - Tone: concise professional English.
        """.trimIndent()

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
        val kept =
            query
                .split(Regex("\\s+"))
                .map(::clean)
                .filter { it.isNotBlank() && it.lowercase() !in FILLER_WORDS }
        return if (kept.isEmpty()) query else kept.joinToString(" ")
    }
}
