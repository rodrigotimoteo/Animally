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
        var successfulToolCalls = 0
        repeat(MAX_TOOL_ROUNDS) {
            val turn = requestTurn(messages)
            if (turn.fallbackToPlainText) {
                return RagToolAnswer(
                    sources = collectedSources.distinctBy { it.recordType to it.recordId },
                    fallbackToPlainText = true,
                    usedAuthoritativeTool = successfulToolCalls > 0,
                )
            }
            if (turn.calls.isEmpty()) {
                val text = sanitize(turn.text)
                return if (text.isBlank() && successfulToolCalls > 0) {
                    RagToolAnswer(
                        sources = collectedSources.distinctBy { it.recordType to it.recordId },
                        fallbackToPlainText = true,
                        usedAuthoritativeTool = true,
                    )
                } else {
                    RagToolAnswer(
                        text = text,
                        sources = collectedSources.distinctBy { it.recordType to it.recordId },
                        usedAuthoritativeTool = successfulToolCalls > 0,
                    )
                }
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
            successfulToolCalls += executeToolCalls(turn.calls, messages, collectedSources)
        }
        return RagToolAnswer(
            sources = collectedSources.distinctBy { it.recordType to it.recordId },
            fallbackToPlainText = true,
            usedAuthoritativeTool = successfulToolCalls > 0,
        )
    }

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
