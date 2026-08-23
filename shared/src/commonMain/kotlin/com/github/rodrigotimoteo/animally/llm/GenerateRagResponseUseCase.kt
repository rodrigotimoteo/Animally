package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.search.usecase.SearchUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.ceil

class GenerateRagResponseUseCase(
    private val searchUseCase: SearchUseCase,
    private val llmEngine: LlmEngine,
    private val config: RagConfig = RagConfig.DEFAULT,
) {
    /** Rough token estimate: ~4 characters per token (see RAG budget in CONTEXT docs). */
    private companion object {
        const val CHARS_PER_TOKEN = 4.0
    }

    operator fun invoke(query: String): Flow<String> =
        flow {
            val enriched = AssistantPrompts.enrichQuery(query)
            val results = searchUseCase(enriched, from = null, to = null, recordTypes = null)
            val chunks = results.map { result -> formatChunk(result) }
            val context = buildContext(chunks, query)
            llmEngine.generate(context, AssistantPrompts.SYSTEM_PROMPT).collect { emit(it) }
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
     * Assembles the user-turn prompt: retrieved context plus the raw question.
     * Role/scope/citation rules live in [AssistantPrompts.SYSTEM_PROMPT] and
     * are passed as instructions, not inline.
     */
    private fun buildContext(
        chunks: List<String>,
        query: String,
    ): String {
        val reserve = config.systemReserveTokens + config.queryReserveTokens + config.responseReserveTokens
        val budget = config.maxContextTokens - reserve
        val sb = StringBuilder()
        var used = 0
        for (chunk in chunks) {
            val est = ceil(chunk.length / CHARS_PER_TOKEN).toInt()
            if (used + est > budget) break
            sb.appendLine(chunk)
            used += est
        }
        val prompt = StringBuilder()
        prompt.appendLine("Context:")
        prompt.appendLine(sb.toString().trimEnd())
        prompt.append("---")
        prompt.appendLine()
        prompt.append("Question: ").append(query)
        return prompt.toString()
    }
}
