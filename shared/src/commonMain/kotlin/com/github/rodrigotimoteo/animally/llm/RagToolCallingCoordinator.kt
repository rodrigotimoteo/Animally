package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

/**
 * Coordinates bounded native-tool turns independently from retrieval and UI
 * citation handling. It only receives provider-neutral messages and returns
 * provider-neutral sources, keeping execution in shared Kotlin.
 */
internal class RagToolCallingCoordinator(
    private val engine: RagToolCallingEngine,
    private val registry: RagToolRegistry,
    private val turnStrings: AssistantStrings,
    private val sanitize: (String) -> String,
    private val onText: (String) -> Unit,
    private val emitChunk: suspend (String) -> Unit,
) {
    suspend fun run(messages: MutableList<RagChatMessage>): RagToolAnswer {
        val collectedSources = mutableListOf<SearchResult>()
        val seenCalls = mutableSetOf<String>()
        var successfulToolCalls = 0
        var completedAnswer: RagToolAnswer? = null
        repeat(MAX_TOOL_ROUNDS) {
            if (completedAnswer == null) {
                val round = processTurn(messages, collectedSources, seenCalls, successfulToolCalls)
                successfulToolCalls += round.successfulToolCalls
                completedAnswer = round.answer
            }
        }
        return completedAnswer
            ?: RagToolAnswer(
                sources = collectedSources.distinctBy { it.recordType to it.recordId },
                fallbackToPlainText = true,
                usedAuthoritativeTool = successfulToolCalls > 0,
            )
    }

    private suspend fun processTurn(
        messages: MutableList<RagChatMessage>,
        collectedSources: MutableList<SearchResult>,
        seenCalls: MutableSet<String>,
        successfulToolCalls: Int,
    ): ProcessedToolTurn {
        val turn = requestTurn(messages)
        return when {
            turn.fallbackToPlainText ->
                ProcessedToolTurn(
                    answer = completedAnswer(collectedSources, successfulToolCalls),
                    successfulToolCalls = 0,
                )
            turn.calls.isEmpty() ->
                ProcessedToolTurn(
                    answer = completeTextTurn(turn.text, collectedSources, successfulToolCalls),
                    successfulToolCalls = 0,
                )
            else -> executeFreshCalls(turn, messages, collectedSources, seenCalls, successfulToolCalls)
        }
    }

    private fun completedAnswer(
        collectedSources: List<SearchResult>,
        successfulToolCalls: Int,
    ): RagToolAnswer =
        RagToolAnswer(
            sources = collectedSources.distinctBy { it.recordType to it.recordId },
            fallbackToPlainText = true,
            usedAuthoritativeTool = successfulToolCalls > 0,
        )

    private fun completeTextTurn(
        text: String,
        collectedSources: List<SearchResult>,
        successfulToolCalls: Int,
    ): RagToolAnswer {
        val sanitized = sanitize(text)
        return if (sanitized.isBlank() && successfulToolCalls > 0) {
            completedAnswer(collectedSources, successfulToolCalls)
        } else {
            RagToolAnswer(
                text = sanitized,
                sources = collectedSources.distinctBy { it.recordType to it.recordId },
                usedAuthoritativeTool = successfulToolCalls > 0,
            )
        }
    }

    private suspend fun executeFreshCalls(
        turn: ToolTurn,
        messages: MutableList<RagChatMessage>,
        collectedSources: MutableList<SearchResult>,
        seenCalls: MutableSet<String>,
        successfulToolCalls: Int,
    ): ProcessedToolTurn {
        check(turn.calls.size <= MAX_TOOL_CALLS_PER_ROUND) {
            "The analysis requested too many tools at once."
        }
        val freshCalls = turn.calls.filter { seenCalls.add(toolCallFingerprint(it)) }
        if (freshCalls.isEmpty()) {
            // Some gateways repeatedly return the same call after its tool
            // result has already been replayed. Repeating it wastes the
            // bounded budget and can leave the user with an avoidable
            // analysis-limit message.
            return ProcessedToolTurn(
                answer = completedAnswer(collectedSources, successfulToolCalls),
                successfulToolCalls = 0,
            )
        }
        emitText(turnStrings.searchingPlaceholder)
        messages +=
            RagChatMessage(
                role = RagChatRole.ASSISTANT,
                content = turn.text.takeIf(String::isNotBlank),
                toolCalls = freshCalls,
            )
        return ProcessedToolTurn(
            answer = null,
            successfulToolCalls = executeToolCalls(freshCalls, messages, collectedSources),
        )
    }

    private data class ProcessedToolTurn(
        val answer: RagToolAnswer?,
        val successfulToolCalls: Int,
    )

    private suspend fun requestTurn(messages: List<RagChatMessage>): ToolTurn {
        var modelText = ""
        var toolCalls = emptyList<RagToolCall>()
        try {
            engine.generateStreamingWithTools(messages.toList(), registry.definitions).collect { event ->
                when (event) {
                    is RagToolStreamEvent.Text -> {
                        modelText = event.text
                        emitText(sanitize(modelText))
                    }
                    is RagToolStreamEvent.ToolCalls -> toolCalls += event.calls
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            // A provider can fail while replaying a valid tool result. Let
            // the outer coordinator use the deterministic app context when
            // it is grounded; the caller still refuses if no grounding exists.
            return ToolTurn(modelText, toolCalls, fallbackToPlainText = true)
        }
        return ToolTurn(modelText, toolCalls)
    }

    private suspend fun executeToolCalls(
        calls: List<RagToolCall>,
        messages: MutableList<RagChatMessage>,
        collectedSources: MutableList<SearchResult>,
    ): Int {
        var successfulCalls = 0
        calls.forEach { call ->
            val result = registry.execute(call)
            collectedSources += result.sources
            if (!result.isError) successfulCalls++
            messages +=
                RagChatMessage(
                    role = RagChatRole.TOOL,
                    content = result.content,
                    toolCallId = call.id,
                    name = call.name,
                )
        }
        return successfulCalls
    }

    private suspend fun emitText(text: String) {
        onText(text)
        emitChunk(text)
    }

    private fun toolCallFingerprint(call: RagToolCall): String {
        val normalizedArguments = call.arguments.replace(Regex("\\s+"), "")
        return "${call.name.trim().lowercase()}|$normalizedArguments"
    }

    private data class ToolTurn(
        val text: String,
        val calls: List<RagToolCall>,
        val fallbackToPlainText: Boolean = false,
    )

    private companion object {
        const val MAX_TOOL_ROUNDS = 3
        const val MAX_TOOL_CALLS_PER_ROUND = 4
    }
}

internal data class RagToolAnswer(
    val text: String = "",
    val sources: List<SearchResult> = emptyList(),
    val fallbackToPlainText: Boolean = false,
    val usedAuthoritativeTool: Boolean = false,
)
