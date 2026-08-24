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
}

class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: RagLlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
) {
    /** Rough token estimate: ~4 characters per token (see RAG budget in CONTEXT docs). */
    private companion object {
        const val CHARS_PER_TOKEN = 4.0

        // Markdown link: [any text without ]]( any url without ) )
        val linkRegex = Regex("\\[([^\\]]*)]\\(([^)]*)\\)")

        /**
         * Deterministic no-context answer. Emitted WITHOUT calling the model:
         * an empty context is exactly where small models hallucinate (they
         * fill the gap with outside knowledge), so we never give them the
         * chance. Also prevents prompt-echo leakage of internal markers.
         */
        const val NO_RESULTS_FALLBACK =
            "I couldn't find anything about that in your records. Try asking " +
                "about a horse by name, a treatment, vaccination, or a date."
    }

    operator fun invoke(query: String): Flow<String> =
        flow {
            val enriched = AssistantPrompts.enrichQuery(query)
            val results = searchUseCase(enriched, from = null, to = null, recordTypes = null)
            val selected = selectWithinBudget(results.map { result -> formatChunk(result) })
            if (selected.isEmpty()) {
                emit(NO_RESULTS_FALLBACK)
                return@flow
            }
            val context = buildContext(selected, query)
            llmEngine.generate(context, AssistantPrompts.SYSTEM_PROMPT).collect { emit(sanitize(it)) }
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
     * Keeps the chunks that fit the token budget. An empty result means every
     * chunk was filtered out (or there were none), which routes to the
     * deterministic fallback instead of a model call.
     */
    private fun selectWithinBudget(chunks: List<String>): List<String> {
        val reserve = config.systemReserveTokens + config.queryReserveTokens + config.responseReserveTokens
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
     * Assembles the user-turn prompt: retrieved context plus the raw question.
     * Role/scope/citation rules live in [AssistantPrompts.SYSTEM_PROMPT] and
     * are passed as instructions, not inline.
     */
    private fun buildContext(
        chunks: List<String>,
        query: String,
    ): String {
        val sb = StringBuilder()
        for (chunk in chunks) {
            sb.appendLine(chunk)
        }
        val prompt = StringBuilder()
        prompt.appendLine("Context:")
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
            .replace(linkRegex, "$1")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
}
