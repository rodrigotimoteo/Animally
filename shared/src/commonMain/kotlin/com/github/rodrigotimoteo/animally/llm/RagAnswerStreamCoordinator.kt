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
            .mapNotNull { match ->
                citationRecordType(match.groupValues[1])?.let { type ->
                    byKey["$type#${match.groupValues[2]}"]
                }
            }.distinct()
            .toList()
    }

    /** Accepts both the wire form and humanized forms a model may produce. */
    private fun citationRecordType(rawType: String): String? {
        val displayType = rawType.trim().replace(Regex("\\s+"), " ")
        val wireType = displayType.uppercase().replace(' ', '_')
        return RecordType.fromWireName(wireType)?.wireName
            ?: RecordType.fromDisplayName(displayType)?.wireName
    }

    private fun sanitize(text: String): String =
        text
            .replace(scaffoldLineRegex, "")
            .replace(linkRegex) { link ->
                val label = link.groupValues[1]
                // Preserve citation-shaped Markdown as an internal marker
                // until source mapping runs. Ordinary links still become
                // their readable label without exposing the URL.
                if (citationReferenceRegex.matches(label.trim())) {
                    "[${label.trim()}]"
                } else {
                    label
                }
            }.replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    private fun stripCitationTokens(text: String): String {
        val withoutCitationBlocks =
            text.replace(citationBlockRegex) { block ->
                val references = citationReferenceRegex.findAll(block.value).toList()
                // A citation is transport syntax, not user-facing prose. Drop
                // the complete bracket even when a model decorates it with
                // text such as `— internal reference`; the real record is
                // represented by the tappable source card emitted below.
                if (references.isNotEmpty()) "" else block.value
            }

        return withoutCitationBlocks
            // A cumulative stream can briefly end halfway through a citation
            // before the closing bracket arrives. Hide that transport syntax
            // from the live bubble as well; the completed source card is still
            // emitted once the final snapshot can be mapped.
            .replace(incompleteCitationRegex, "")
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
        // bracket: [TYPE #id], [TYPE NAME #id], or [TYPE #1, TYPE #2]. Parse
        // references only inside square brackets so ordinary prose containing
        // "TYPE #1" does not accidentally become a source card. Case and
        // spaces are intentionally accepted because models often humanize the
        // internal wire name ("FARRIER VISIT" instead of "FARRIER_VISIT").
        val citationBlockRegex = Regex("\\[[^]]*]")
        val citationReferenceRegex =
            Regex("([A-Z][A-Z_]*(?:\\s+[A-Z_]+)*)\\s*#(\\d+)", RegexOption.IGNORE_CASE)
        val incompleteCitationRegex =
            Regex(
                "\\[(?:[A-Z][A-Z_]*(?:\\s+[A-Z_]+)*)\\s*#\\d*[^]]*$",
                RegexOption.IGNORE_CASE,
            )

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
