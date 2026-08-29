package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
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
    val requiresGrounding: Boolean,
    val grounded: Boolean,
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
        val answer = streamGeneratedAnswer(collector, request) ?: return
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

    /** Enforces source citations and emits source cards for a completed turn. */
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
        llmEngine
            .generateStreaming(
                request.context,
                AssistantPrompts.systemPrompt(
                    request.turnStrings,
                    allowGeneralQuestions = request.allowGeneralQuestions,
                ),
            ).collect { text ->
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
                        ) +
                            "\nUse the read-only analysis tools when they improve accuracy. " +
                            "Tool results are authoritative for this app's data. Never invent a source header; " +
                            "when a tool result includes a source field, cite that exact [TYPE #ID] value."
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
        return citationBlockRegex
            .findAll(answerText)
            .flatMap { block -> citationReferenceRegex.findAll(block.value) }
            .mapNotNull { match -> byKey["${match.groupValues[1]}#${match.groupValues[2]}"] }
            .distinct()
            .toList()
    }

    private fun sanitize(text: String): String =
        text
            .replace(scaffoldLineRegex, "")
            .replace(linkRegex, "$1")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    private fun stripCitationTokens(text: String): String {
        val withoutCitationBlocks =
            text.replace(citationBlockRegex) { block ->
                val references = citationReferenceRegex.findAll(block.value).toList()
                val remainder =
                    citationReferenceRegex
                        .replace(block.value, "")
                        .replace(Regex("(?i)\\b(?:and|or)\\b"), "")
                        .replace(Regex("[\\[\\],;|&/]"), "")
                        .trim()
                if (references.isNotEmpty() && remainder.isEmpty()) "" else block.value
            }

        return withoutCitationBlocks
            .replace(literalTagRegex, "")
            .replace(multiSpaceRegex, " ")
            .replace(spacedRepeatedPunctuationRegex, "$1")
            .replace(spaceBeforePunctuationRegex, "$1")
            .replace(lineLeadingSpaceRegex, "")
            .replace(blankLineRunRegex, "\n\n")
            .trim()
    }

    private companion object {
        // Markdown link: [any text without ]]( any url without ) )
        val linkRegex = Regex("\\[([^\\]]*)]\\(([^)]*)\\)")

        // Model sometimes regurgitates prompt scaffolding.
        val scaffoldLineRegex = Regex("(?m)^\\s*(?:-{3,}|Question:.*|Context:.*|You are .*)\\s*\\n?")

        // A model may cite one record or group several record headers in one
        // bracket: [TYPE #id] or [TYPE #1, TYPE #2]. Parse references only
        // inside square brackets so ordinary prose containing "TYPE #1" does
        // not accidentally become a source card.
        val citationBlockRegex = Regex("\\[[^]]*]")
        val citationReferenceRegex = Regex("([A-Z_]+)\\s*#(\\d+)")

        // Internal tags must never reach the user-facing bubble.
        val literalTagRegex =
            Regex(
                """\[(?:Summary|RECORD_TYPE #ID|PATIENT CENSUS|CARE COUNTS|GESTATIONS|OVERDUE CARE[^]]*)]""",
            )

        val multiSpaceRegex = Regex("[ \\t]{2,}")
        val spaceBeforePunctuationRegex = Regex("[ \\t]+([.,;:!?])")
        val spacedRepeatedPunctuationRegex = Regex("([.!?])([ \\t]+\\1)+")
        val lineLeadingSpaceRegex = Regex("(?m)^[ \\t]+")
        val blankLineRunRegex = Regex("\\n{3,}")

        const val MAX_ENFORCED_SOURCES = 3
    }
}
