package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/** A provider-neutral function schema exposed to a cloud model. */
data class RagToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

/** A function call proposed by a model. Arguments stay raw until Kotlin validates them. */
data class RagToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    /** App-owned scope bound immediately before executing this call. */
    val executionScope: RagToolExecutionScope? = null,
)

/** Deterministic authorization context for one model-generated tool call. */
data class RagToolExecutionScope(
    val resolvedPatientId: Long?,
    val requiresPatientName: Boolean,
)

/** Structured result returned to the model after a tool executes. */
data class RagToolResult(
    val toolCallId: String,
    val name: String,
    val content: String,
    val sources: List<SearchResult> = emptyList(),
    val isError: Boolean = false,
)

/** Provider-neutral events emitted by one tool-aware model request. */
sealed interface RagToolStreamEvent {
    /** Cumulative visible answer text. */
    data class Text(
        val text: String,
    ) : RagToolStreamEvent

    /** One complete batch of function calls, emitted after the model finishes its turn. */
    data class ToolCalls(
        val calls: List<RagToolCall>,
    ) : RagToolStreamEvent
}

/** Chat roles needed to replay an assistant/tool exchange to a chat-completions endpoint. */
enum class RagChatRole(
    val wireName: String,
) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
}

/** Provider-neutral conversation message used by the tool-call loop. */
data class RagChatMessage(
    val role: RagChatRole,
    val content: String? = null,
    val toolCalls: List<RagToolCall> = emptyList(),
    val toolCallId: String? = null,
    val name: String? = null,
)

/** Optional capability implemented by cloud engines that support native function calling. */
interface RagToolCallingEngine {
    /** True when the configured transport can send native tool schemas. */
    val supportsToolCalling: Boolean

    /** Streams one model turn, returning either visible text or completed tool calls. */
    fun generateStreamingWithTools(
        messages: List<RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent>
}

/** Shared Kotlin boundary for safe, application-owned tool execution. */
interface RagToolRegistry {
    val definitions: List<RagToolDefinition>

    suspend fun execute(call: RagToolCall): RagToolResult
}
