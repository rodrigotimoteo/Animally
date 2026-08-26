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
        var executedToolCall = false
        repeat(MAX_TOOL_ROUNDS) {
            val turn = requestTurn(messages, executedToolCall)
            if (turn.fallbackToPlainText) {
                return RagToolAnswer(fallbackToPlainText = true)
            }
            if (turn.calls.isEmpty()) {
                return RagToolAnswer(
                    text = sanitize(turn.text),
                    sources = collectedSources.distinctBy { it.recordType to it.recordId },
                )
            }
            check(turn.calls.size <= MAX_TOOL_CALLS_PER_ROUND) {
                "The analysis requested too many tools at once."
            }
            emitText(turnStrings.searchingPlaceholder)
            messages +=
                RagChatMessage(
                    role = RagChatRole.ASSISTANT,
                    content = turn.text.takeIf(String::isNotBlank),
                    toolCalls = turn.calls,
                )
            executeToolCalls(turn.calls, messages, collectedSources)
            executedToolCall = true
        }
        val limitReply = turnStrings.analysisLimitReply
        emitText(limitReply)
        return RagToolAnswer(
            text = limitReply,
            sources = collectedSources.distinctBy { it.recordType to it.recordId },
        )
    }

    private suspend fun requestTurn(
        messages: List<RagChatMessage>,
        hasExecutedToolCall: Boolean,
    ): ToolTurn {
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
        } catch (t: Throwable) {
            if (!hasExecutedToolCall) return ToolTurn(modelText, toolCalls, fallbackToPlainText = true)
            throw t
        }
        return ToolTurn(modelText, toolCalls)
    }

    private suspend fun executeToolCalls(
        calls: List<RagToolCall>,
        messages: MutableList<RagChatMessage>,
        collectedSources: MutableList<SearchResult>,
    ) {
        calls.forEach { call ->
            val result = registry.execute(call)
            collectedSources += result.sources
            messages +=
                RagChatMessage(
                    role = RagChatRole.TOOL,
                    content = result.content,
                    toolCallId = call.id,
                    name = call.name,
                )
        }
    }

    private suspend fun emitText(text: String) {
        onText(text)
        emitChunk(text)
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
)
