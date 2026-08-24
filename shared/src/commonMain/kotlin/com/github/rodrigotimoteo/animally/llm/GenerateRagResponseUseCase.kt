package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.ceil

/**
 * Narrow generation seam so the RAG pipeline can be tested without the
 * platform [LlmEngine] expect class (which cannot be instantiated or faked
 * from common code). Production wiring adapts [LlmEngine] to this in
 * LlmModule.
 */
fun interface RagLlmEngine {
    fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String>

    /**
     * Streaming variant emitting CUMULATIVE text (full response so far per value).
     * Defaults to a single-emission stream over [generate] so existing fakes keep
     * working unchanged.
     */
    fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> = generate(prompt, instructions)
}

/**
 * One prior conversational turn fed back into the prompt for multi-turn
 * context. Both sides are truncated by the use case before prompting.
 */
data class RagHistoryEntry(
    val question: String,
    val answer: String,
)

/**
 * Broad-retrieval seam for the OR retry. Deliberately separate from
 * [SearchUseCase]: the use case's tokenizer stars every whitespace token,
 * which corrupts boolean operators ("OR" -> "OR*", an FTS5 syntax error).
 * Implementations receive an ALREADY FTS5-shaped expression from
 * [AssistantPrompts.toFtsOrQuery] and must pass it to the repository untouched.
 */
fun interface RagOrSearch {
    fun search(ftsQuery: String): List<SearchResult>
}

class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: RagLlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
    private val strings: AssistantStrings = EnAssistantStrings,
    private val orSearch: RagOrSearch? = null,
) {
    /** Rough token estimate: ~4 characters per token (see RAG budget in CONTEXT docs). */
    private companion object {
        const val CHARS_PER_TOKEN = 4.0

        // Markdown link: [any text without ]]( any url without ) )
        val linkRegex = Regex("\\[([^\\]]*)]\\(([^)]*)\\)")

        const val MIN_QUERY_CHARS = 2

        // Multi-turn context: keep at most this many prior Q/A pairs and
        // truncate each side so one verbose turn cannot eat the budget.
        const val MAX_HISTORY_ENTRIES = 3
        const val MAX_HISTORY_SIDE_CHARS = 200

        // Model sometimes regurgitates prompt scaffolding (--- separators,
        // "Question: ..." echoes). Stripped defensively from every chunk.
        val scaffoldLineRegex = Regex("(?m)^\\s*(?:-{3,}|Question:.*|Context:.*|You are .*)\\s*\\n?")
    }

    /**
     * Asks [query] against the record corpus, optionally grounded in
     * [history] (prior Q/A pairs, most recent last). Streaming emits
     * cumulative sanitized snapshots of the reply.
     */
    operator fun invoke(
        query: String,
        history: List<RagHistoryEntry> = emptyList(),
    ): Flow<String> =
        flow {
            // Language mirroring: the device locale picks the default strings,
            // but a Portuguese question gets a Portuguese turn even on an EN
            // device - answering "Quantos pacientes tenho?" in English reads
            // as broken to a PT-speaking vet.
            val turnStrings =
                if (AssistantPrompts.isPortugueseQuery(query)) PtAssistantStrings else strings
            // Greetings/small talk never touch retrieval - searching "hi" in
            // veterinary records and answering with the no-results fallback
            // reads as broken to the user.
            AssistantPrompts.greetingReply(query, turnStrings)?.let {
                emit(it)
                return@flow
            }
            val enriched = AssistantPrompts.enrichQuery(query)
            if (enriched.length < MIN_QUERY_CHARS) {
                // One-letter queries fuzzy-match nonsense ("A" hits any name
                // containing A); asking for more beats answering garbage.
                emit(turnStrings.tooShortReply)
                return@flow
            }
            val results =
                searchUseCase(enriched, from = null, to = null, recordTypes = null)
                    .ifEmpty {
                        // AND semantics require every content word to match, so
                        // natural questions like "which patients belong to X"
                        // zero out on words like "belong". One broad OR retry
                        // over an FTS-safe expression recovers broad matches.
                        orSearch?.search(AssistantPrompts.toFtsOrQuery(query)).orEmpty()
                    }
            val recentConversation = formatHistory(history)
            val selected =
                selectWithinBudget(
                    results.map { result -> formatChunk(result) },
                    reservedTokens = estimateTokens(recentConversation),
                )
            if (selected.isEmpty() && recentConversation.isEmpty()) {
                emit(turnStrings.noResultsFallback)
                return@flow
            }
            val context = buildContext(selected, query, recentConversation)
            // Streaming emits cumulative snapshots; sanitize() is idempotent, so
            // re-sanitizing the growing text each step is safe and downstream
            // consumers replace their buffer with each emission.
            var lastEmitted = ""
            llmEngine
                .generateStreaming(context, AssistantPrompts.systemPrompt(turnStrings))
                .collect { text ->
                    lastEmitted = sanitize(text)
                    emit(lastEmitted)
                }
            // Citation enforcement: the system prompt mandates citing bracketed
            // headers, but the model skips them often enough that the guarantee
            // is enforced here - when records were used and the reply carries
            // none, append the ACTUAL retrieved headers (never invented ones).
            if (selected.isNotEmpty() && "[" !in lastEmitted) {
                val sources = selected.mapNotNull(::sourceHeader)
                if (sources.isNotEmpty()) {
                    emit(
                        listOf(lastEmitted.takeIf(String::isNotBlank), sources.joinToString("\n"))
                            .filterNotNull()
                            .joinToString("\n\n"),
                    )
                }
            }
        }

    /**
     * Formats one search hit as a citable source block. The bracketed header
     * carries record id and patient id so the model can cite precisely; the
     * system prompt tells the model these headers are source references.
     */
    private fun formatChunk(result: SearchResult): String {
        val date = result.date?.toString() ?: "unknown date"
        val breed = result.breed ?: "unknown breed"
        return buildString {
            val header = "[${result.recordType} #${result.recordId}] ${result.patientName} ($breed, $date)"
            appendLine("$header | patient #${result.patientId}")
            appendLine(result.snippet)
        }
    }

    /**
     * Keeps the chunks that fit the token budget after reserving room for the
     * system prompt, the query, the response, and [reservedTokens] for any
     * recent-conversation block. An empty result means every chunk was
     * filtered out (or there were none).
     */
    private fun selectWithinBudget(
        chunks: List<String>,
        reservedTokens: Int = 0,
    ): List<String> {
        val reserve =
            config.systemReserveTokens + config.queryReserveTokens +
                config.responseReserveTokens + reservedTokens
        val budget = config.maxContextTokens - reserve
        val selected = mutableListOf<String>()
        var used = 0
        for (chunk in chunks) {
            val est = ceil(chunk.length / CHARS_PER_TOKEN).toInt()
            if (used + est > budget) break
            selected.add(chunk)
            used += est
        }
        return selected
    }

    /**
     * Renders prior Q/A pairs as a compact transcript block: at most
     * [MAX_HISTORY_ENTRIES] most-recent pairs, each side truncated to
     * [MAX_HISTORY_SIDE_CHARS]. Empty when there is no history.
     */
    private fun formatHistory(history: List<RagHistoryEntry>): String {
        val recent = history.takeLast(MAX_HISTORY_ENTRIES)
        if (recent.isEmpty()) return ""
        return buildString {
            appendLine("Recent conversation:")
            for (entry in recent) {
                append("User: ").appendLine(entry.question.take(MAX_HISTORY_SIDE_CHARS))
                append("Assistant: ").appendLine(entry.answer.take(MAX_HISTORY_SIDE_CHARS))
            }
        }.trimEnd()
    }

    private fun estimateTokens(text: String): Int = ceil(text.length / CHARS_PER_TOKEN).toInt()

    /**
     * Extracts the citable header ("[TYPE #id] Name") from a formatted chunk,
     * or null when the chunk has no header line. Used by the citation
     * enforcement fallback - only headers of chunks actually retrieved are
     * ever appended.
     */
    private fun sourceHeader(chunk: String): String? =
        chunk
            .lineSequence()
            .firstOrNull()
            ?.substringBefore(" |")
            ?.takeIf { it.startsWith("[") }

    /**
     * Assembles the user-turn prompt: optional recent conversation for
     * multi-turn context, retrieved context, then the raw question.
     * Role/scope/citation rules live in the system prompt and are passed as
     * instructions, not inline.
     */
    private fun buildContext(
        chunks: List<String>,
        query: String,
        recentConversation: String = "",
    ): String {
        val prompt = StringBuilder()
        if (recentConversation.isNotEmpty()) {
            prompt.appendLine(recentConversation)
            prompt.appendLine("---")
        }
        prompt.appendLine("Context:")
        val sb = StringBuilder()
        for (chunk in chunks) {
            sb.appendLine(chunk)
        }
        prompt.appendLine(sb.toString().trimEnd())
        prompt.append("---")
        prompt.appendLine()
        prompt.append("Question: ").append(query)
        return prompt.toString()
    }

    /**
     * Strips markdown the model was told not to produce but sometimes does:
     * bold markers (** and __), backticks, and [text](url) links reduced to
     * their text. Applied to every emitted chunk before it reaches the UI.
     */
    private fun sanitize(text: String): String =
        text
            .replace(scaffoldLineRegex, "")
            .replace(linkRegex, "$1")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}
