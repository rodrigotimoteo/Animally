package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagChatMessage
import com.github.rodrigotimoteo.animally.llm.RagToolCall
import com.github.rodrigotimoteo.animally.llm.RagToolDefinition
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Configuration for the OpenAI-compatible chat-completions endpoint.
 * The API key stays separate so this data class remains loggable.
 */
data class CloudLlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val apiKey: String,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
    val maxTokens: Int? = null,
    /** True only for an explicitly selected local runtime such as Ollama. */
    val allowInsecureLocalEndpoint: Boolean = false,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 120_000L
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 180_000L
        const val DEFAULT_MAX_TOKENS = 2048
    }
}

private const val ROLE_SYSTEM = "system"
private const val ROLE_USER = "user"
private const val TOOL_TYPE_FUNCTION = "function"
private const val TOOL_CHOICE_AUTO = "auto"

/** Pure request-DTO builder so contract tests can assert the wire shape. */
internal fun buildChatCompletionRequest(
    config: CloudLlmConfig,
    prompt: String,
    instructions: String,
): ChatCompletionRequest {
    val messages =
        listOf(
            ChatMessage(role = ROLE_SYSTEM, content = instructions),
            ChatMessage(role = ROLE_USER, content = prompt),
        )
    return ChatCompletionRequest(
        model = config.model,
        messages = messages,
        stream = true,
        maxTokens = config.maxTokens,
        tools = null,
        toolChoice = null,
    )
}

/** Builds the wire request used for one turn of native function calling. */
internal fun buildToolChatCompletionRequest(
    config: CloudLlmConfig,
    messages: List<RagChatMessage>,
    tools: List<RagToolDefinition>,
): ChatCompletionRequest =
    ChatCompletionRequest(
        model = config.model,
        messages = messages.map(RagChatMessage::toCloudMessage),
        stream = true,
        maxTokens = config.maxTokens,
        tools = tools.map(::toCloudTool),
        toolChoice = TOOL_CHOICE_AUTO,
    )

internal fun applyCloudLlmRequest(
    builder: HttpRequestBuilder,
    config: CloudLlmConfig,
    prompt: String,
    instructions: String,
) {
    if (shouldSendCloudAuthorization(config)) {
        builder.headers.append(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
    }
    builder.contentType(ContentType.Application.Json)
    builder.accept(ContentType.Text.EventStream)
    builder.timeout {
        socketTimeoutMillis = config.socketTimeoutMillis
        connectTimeoutMillis = config.connectTimeoutMillis
        requestTimeoutMillis = config.requestTimeoutMillis
    }
    builder.setBody(buildChatCompletionRequest(config, prompt, instructions))
}

internal fun applyCloudLlmRequest(
    builder: HttpRequestBuilder,
    config: CloudLlmConfig,
    request: ChatCompletionRequest,
) {
    if (shouldSendCloudAuthorization(config)) {
        builder.headers.append(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
    }
    builder.contentType(ContentType.Application.Json)
    builder.accept(ContentType.Text.EventStream)
    builder.timeout {
        socketTimeoutMillis = config.socketTimeoutMillis
        connectTimeoutMillis = config.connectTimeoutMillis
        requestTimeoutMillis = config.requestTimeoutMillis
    }
    builder.setBody(request)
}

private fun shouldSendCloudAuthorization(config: CloudLlmConfig): Boolean = config.apiKey.isNotBlank() && !isInsecureCloudBaseUrl(config.baseUrl)

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
    @SerialName("max_tokens") val maxTokens: Int?,
    val tools: List<ChatCompletionTool>?,
    @SerialName("tool_choice") val toolChoice: String?,
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String?,
    @SerialName("tool_calls") val toolCalls: List<ChatToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null,
)

@Serializable
internal data class ChatCompletionTool(
    val type: String,
    val function: ChatFunctionDefinition,
)

@Serializable
internal data class ChatFunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
internal data class ChatToolCall(
    val id: String? = null,
    val type: String? = null,
    val function: ChatFunctionCall? = null,
    val index: Int? = null,
)

@Serializable
internal data class ChatFunctionCall(
    val name: String? = null,
    val arguments: String? = null,
)

@Serializable
internal data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList(),
    val event: String? = null,
    val type: String? = null,
    val status: String? = null,
    val done: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
    val error: kotlinx.serialization.json.JsonElement? = null,
    val cost: kotlinx.serialization.json.JsonElement? = null,
    val usage: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
internal data class ChunkChoice(
    val delta: Delta? = null,
    val message: CompletionMessage? = null,
    val text: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class CompletionMessage(
    val content: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("tool_calls") val toolCalls: List<ChatToolCall>? = null,
    @SerialName("reasoning_content") val reasoningContent: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("reasoning_details") val reasoningDetails: kotlinx.serialization.json.JsonElement? = null,
    val reasoning: kotlinx.serialization.json.JsonElement? = null,
    val thinking: kotlinx.serialization.json.JsonElement? = null,
    val analysis: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
internal data class Delta(
    val content: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("reasoning_content") val reasoningContent: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("reasoning_details") val reasoningDetails: kotlinx.serialization.json.JsonElement? = null,
    val reasoning: kotlinx.serialization.json.JsonElement? = null,
    val thinking: kotlinx.serialization.json.JsonElement? = null,
    val analysis: kotlinx.serialization.json.JsonElement? = null,
    @SerialName("tool_calls") val toolCalls: List<ChatToolCall>? = null,
)

private fun RagChatMessage.toCloudMessage(): ChatMessage =
    ChatMessage(
        role = role.wireName,
        content = content,
        toolCalls = toolCalls.map { call -> call.toCloudToolCall() }.takeIf { it.isNotEmpty() },
        toolCallId = toolCallId,
        name = name,
    )

private fun RagToolCall.toCloudToolCall(): ChatToolCall =
    ChatToolCall(
        id = id,
        type = TOOL_TYPE_FUNCTION,
        function = ChatFunctionCall(name = name, arguments = arguments),
    )

private fun toCloudTool(definition: RagToolDefinition): ChatCompletionTool {
    val function =
        ChatFunctionDefinition(
            name = definition.name,
            description = definition.description,
            parameters = definition.parameters,
        )
    return ChatCompletionTool(type = TOOL_TYPE_FUNCTION, function = function)
}
