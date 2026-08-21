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
            val results = searchUseCase(query, from = null, to = null, recordTypes = null)
            val chunks = results.map { result -> formatChunk(result) }
            val context = buildContext(chunks, query)
            llmEngine.generate(context).collect { emit(it) }
        }

    private fun formatChunk(result: SearchResult): String {
        val date = result.date?.toString() ?: "unknown date"
        val breed = result.breed ?: "unknown breed"
        return buildString {
            appendLine("[${result.recordType}] ${result.patientName} ($breed, $date)")
            appendLine(result.snippet)
        }
    }

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
        prompt.appendLine("You are Animally Assistant. Answer using only the context below. If unknown, say so.")
        prompt.appendLine("---")
        prompt.appendLine("Context:")
        prompt.appendLine(sb.toString().trimEnd())
        prompt.appendLine("---")
        prompt.append("Question: ").append(query)
        return prompt.toString()
    }
}
