package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect

/** Provider-neutral request assembled by the RAG use case for one answer turn. */
internal data class RagStreamRequest(
    val context: String,
    val turnStrings: AssistantStrings,
    val selected: List<String>,
    val contextResults: List<SearchResult>,
    val usedDeterministicSummary: Boolean,
    val allowGeneralQuestions: Boolean,
    val useTools: Boolean,
    val forceCloud: Boolean,
    val requiresGrounding: Boolean,
    val grounded: Boolean,
    val webSources: List<VeterinaryWebSource>,
    val webReferencesUnavailable: Boolean = false,
    val toolExecutionScope: RagToolExecutionScope? = null,
)

/**
 * Owns model streaming, bounded analysis-tool turns, and citation rendering.
 * Keeping this boundary separate lets the use case focus on routing and
 * grounding decisions while all provider-neutral business rules remain shared.
 */
internal class RagAnswerStreamCoordinator(
    private val llmEngine: RagLlmEngine,
    private val toolCallingEngine: RagToolCallingEngine?,
    private val toolRegistry: RagToolRegistry?,
) {
    private data class StreamedAnswer(
        val text: String,
        val toolSources: List<SearchResult> = emptyList(),
    )

    /** Streams an answer, adds authoritative source cards, then hides citation syntax in the bubble. */
    suspend fun stream(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
    ) {
        if (request.webSources.isNotEmpty()) {
            collector.emit(RagStreamEvent.WebSources(request.webSources))
        }
        val answer = streamGeneratedAnswer(collector, request) ?: return
        if (request.webSources.isNotEmpty() && !hasValidWebCitation(answer.text, request.webSources.size)) {
            collector.emit(RagStreamEvent.Chunk(webReferenceExcerptFallback(request)))
            return
        }
        val streamedText = answer.text
        emitCitationEvents(collector, request, answer)
        val displayText = stripCitationTokens(streamedText)
        if (displayText != streamedText) {
            collector.emit(RagStreamEvent.Chunk(displayText))
        }
    }

    /** Streams a model/tool turn, rejecting unsupported grounded answers. */
    private suspend fun streamGeneratedAnswer(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
    ): StreamedAnswer? {
        // Streaming emits cumulative snapshots; sanitize() is idempotent, so
        // re-sanitizing the growing text is safe for the UI buffer.
        var lastEmitted = ""
        return try {
            val answer = streamModelAnswer(collector, request) { text -> lastEmitted = text }
            if (answer.fallbackToPlainText) {
                if (request.requiresGrounding && !request.grounded) {
                    collector.emit(RagStreamEvent.Chunk(request.turnStrings.analysisLimitReply))
                    null
                } else {
                    StreamedAnswer(
                        text = streamPlainText(collector, request) { text -> lastEmitted = text },
                        toolSources = answer.sources,
                    )
                }
            } else if (request.requiresGrounding && !request.grounded && !answer.usedAuthoritativeTool) {
                collector.emit(RagStreamEvent.Chunk(request.turnStrings.analysisLimitReply))
                null
            } else {
                StreamedAnswer(answer.text, answer.sources)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            // Mid-stream failure (engine error, not user cancellation):
            // surface a typed marker carrying the partial text so the UI
            // can offer a retry instead of showing a dead bubble.
            collector.emit(RagStreamEvent.Interrupted(partialText = lastEmitted, error = t.message))
            null
        }
    }

    /** Resolves hidden source markers and emits source cards for a completed turn. */
    private suspend fun emitCitationEvents(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
        answer: StreamedAnswer,
    ) {
        var finalText = answer.text
        // Citation enforcement: the system prompt mandates citing bracketed
        // headers, but the model skips them often enough that the guarantee
        // is enforced here - when records were used and the reply carries none,
        // append the ACTUAL retrieved headers (never invented ones).
        val allContextResults =
            (request.contextResults + answer.toolSources).distinctBy { it.recordType to it.recordId }
        var mappedCitations = citedResults(finalText, allContextResults)
        if (mappedCitations.isEmpty()) {
            val candidateSources =
                if (answer.toolSources.isNotEmpty()) {
                    answer.toolSources.map(::sourceHeader)
                } else {
                    request.selected.mapNotNull(::sourceHeader)
                }
            val sources = candidateSources.distinct().take(MAX_ENFORCED_SOURCES)
            if (sources.isNotEmpty()) {
                finalText =
                    listOf(finalText.takeIf(String::isNotBlank), sources.joinToString("\n"))
                        .filterNotNull()
                        .joinToString("\n\n")
                mappedCitations = citedResults(finalText, allContextResults)
            }
        }
        // Summary-only answers have no record headers to append. The literal
        // [Summary] tag keeps the computed source visible to the UI contract.
        if (request.usedDeterministicSummary && mappedCitations.isEmpty() && "[Summary]" !in finalText) {
            finalText =
                listOf(finalText.takeIf(String::isNotBlank), "[Summary]")
                    .filterNotNull()
                    .joinToString("\n\n")
        }
        citedResults(finalText, allContextResults).takeIf { it.isNotEmpty() }?.let {
            collector.emit(RagStreamEvent.Sources(it))
        }
    }

    /** Streams a normal text-only model request and returns its final snapshot. */
    private suspend fun streamPlainText(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
        onText: (String) -> Unit = {},
    ): String {
        var lastEmitted = ""
        val stream =
            if (request.forceCloud || request.useTools) {
                llmEngine.generateCloudFirst(
                    request.context,
                    AssistantPrompts.systemPrompt(
                        request.turnStrings,
                        allowGeneralQuestions = request.allowGeneralQuestions,
                        includeWebReferences = request.webSources.isNotEmpty(),
                        webReferencesUnavailable = request.webReferencesUnavailable,
                    ),
                )
            } else {
                llmEngine.generateStreaming(
                    request.context,
                    AssistantPrompts.systemPrompt(
                        request.turnStrings,
                        allowGeneralQuestions = request.allowGeneralQuestions,
                        includeWebReferences = request.webSources.isNotEmpty(),
                        webReferencesUnavailable = request.webReferencesUnavailable,
                    ),
                )
            }
        stream.collect { text ->
            lastEmitted = sanitize(text)
            onText(lastEmitted)
            collector.emit(RagStreamEvent.Chunk(stripCitationTokens(lastEmitted)))
        }
        return lastEmitted
    }

    /** Streams either a normal answer or a bounded native-tool answer. */
    private suspend fun streamModelAnswer(
        collector: FlowCollector<RagStreamEvent>,
        request: RagStreamRequest,
        onText: (String) -> Unit = {},
    ): RagToolAnswer {
        val toolAnswer =
            if (request.useTools) {
                val engine = toolCallingEngine
                val registry = toolRegistry
                if (engine == null || registry == null) {
                    RagToolAnswer(fallbackToPlainText = true)
                } else {
                    val systemPrompt =
                        AssistantPrompts.systemPrompt(
                            request.turnStrings,
                            allowGeneralQuestions = request.allowGeneralQuestions,
                            includeWebReferences = request.webSources.isNotEmpty(),
                            webReferencesUnavailable = request.webReferencesUnavailable,
                        ) +
                            "\nUse the read-only analysis tools when they improve accuracy. " +
                            "Tool results are authoritative for this app's data. Never invent a source header; " +
                            "when a tool result includes a source field, use it internally so the app can open " +
                            "the matching source card; never print the [TYPE #ID] value."
                    val messages =
                        mutableListOf(
                            RagChatMessage(RagChatRole.SYSTEM, content = systemPrompt),
                            RagChatMessage(RagChatRole.USER, content = request.context),
                        )
                    RagToolCallingCoordinator(
                        engine = engine,
                        registry = registry,
                        turnStrings = request.turnStrings,
                        sanitize = ::sanitize,
                        onText = onText,
                        emitChunk = { chunk ->
                            collector.emit(RagStreamEvent.Chunk(stripCitationTokens(chunk)))
                        },
                        executionScope = request.toolExecutionScope,
                    ).run(messages)
                }
            } else {
                null
            }
        return toolAnswer ?: RagToolAnswer(streamPlainText(collector, request, onText), emptyList())
    }

    private fun sourceHeader(chunk: String): String? =
        chunk
            .lineSequence()
            .firstOrNull()
            ?.substringBefore(" |")
            ?.takeIf { it.startsWith("[") }

    private fun sourceHeader(result: SearchResult): String = "[${result.recordType} #${result.recordId}]"

    private fun citedResults(
        answerText: String,
        contextResults: List<SearchResult>,
    ): List<SearchResult> {
        if (contextResults.isEmpty()) return emptyList()
        val byKey = contextResults.associateBy { "${it.recordType}#${it.recordId}" }
        return assistantCitationBlockRegex
            .findAll(answerText)
            .flatMap { block -> assistantCitationReferenceRegex.findAll(block.value) }
            .mapNotNull { match ->
                citationRecordType(match.groupValues[1])?.let { type ->
                    byKey["$type#${match.groupValues[2]}"]
                }
            }.distinct()
            .toList()
    }

    /** Web-backed medical answers must identify one of the supplied excerpts. */
    private fun hasValidWebCitation(
        answerText: String,
        sourceCount: Int,
    ): Boolean =
        webCitationRegex
            .findAll(answerText)
            .mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
            .any { it in 1..sourceCount }

    /** Keeps a janky model from turning verified web context into a rejection. */
    private fun webReferenceExcerptFallback(request: RagStreamRequest): String =
        buildString {
            appendLine(request.turnStrings.webReferenceAnswerUnavailable)
            request.webSources.take(MAX_FALLBACK_WEB_SOURCES).forEachIndexed { index, source ->
                if (index > 0) appendLine()
                append(source.publisher).append(": ").appendLine(source.title)
                appendLine(source.excerpt.take(MAX_FALLBACK_EXCERPT_CHARS))
            }
        }.trim()

    /** Accepts both the wire form and humanized forms a model may produce. */
    private fun citationRecordType(rawType: String): String? {
        val displayType = rawType.trim().replace(Regex("\\s+"), " ")
        val wireType = displayType.uppercase().replace(' ', '_')
        return RecordType.fromWireName(wireType)?.wireName
            ?: RecordType.fromDisplayName(displayType)?.wireName
    }

    private fun sanitize(text: String): String = sanitizeAssistantTransportText(text)

    private fun stripCitationTokens(text: String): String = sanitizeAssistantDisplayText(text)

    private companion object {
        const val MAX_ENFORCED_SOURCES = 3
        const val MAX_FALLBACK_WEB_SOURCES = 2
        const val MAX_FALLBACK_EXCERPT_CHARS = 900
        val webCitationRegex = Regex("\\[WEB\\s*#(\\d+)]", RegexOption.IGNORE_CASE)
    }
}
