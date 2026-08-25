package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Configuration for the OpenAI-compatible chat-completions endpoint used by
 * [CloudRagLlmEngine]. The API key is supplied separately (secure storage) so this
 * data class stays loggable.
 */
data class CloudLlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val apiKey: String,
    /** Socket inactivity timeout; SSE streams may legitimately outlive a total-request cap. */
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 30_000L
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}

/**
 * Data-side RAG engine routing generation to an OpenAI-compatible chat completions
 * endpoint with `stream: true`. Emits CUMULATIVE text (full answer so far per value),
 * matching the contract documented on [RagLlmEngine].
 *
 * Lives beside (not inside) the domain llm seam: domain code only ever sees the
 * RagLlmEngine fun interface, never Ktor types — production wiring injects this
 * implementation through that seam.
 *
 * Any transport/HTTP/parse failure surfaces as a flow error; fallback handling is the
 * caller's job ([FmFirstRagLlmEngine]).
 */
class CloudRagLlmEngine(
    private val httpClient: HttpClient,
    private val configProvider: () -> CloudLlmConfig,
) : RagLlmEngine {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    /**
     * Non-streaming variant. Delegates to [generateStreaming] and takes its final
     * cumulative snapshot, which IS the full response under the cumulative contract.
     */
    override fun generate(
        prompt: String,
        instructions: String,
    ): Flow<String> = generateStreaming(prompt, instructions)

    override fun generateStreaming(
        prompt: String,
        instructions: String,
    ): Flow<String> =
        flow {
            // Resolved per request so settings edits (key/model/URL) apply immediately.
            val config = configProvider()
            httpClient
                .preparePost(config.baseUrl) { applyCloudLlmRequest(this, config, prompt, instructions) }
                .execute { response ->
                    if (!response.status.isSuccess()) {
                        error("Cloud LLM request failed: HTTP ${response.status.value}")
                    }
                    val channel = response.bodyAsChannel()
                    val cumulative = StringBuilder()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        appendSseDelta(line, cumulative)?.let { delta ->
                            if (delta.isNotEmpty()) emit(cumulative.toString())
                        }
                    }
                }
        }

    /**
     * Parses one SSE line; returns the content delta it carries, or null for
     * non-data lines / keep-alive comments / the terminal `[DONE]` sentinel.
     * Internal so contract tests drive the exact production decode path.
     */
    internal fun appendSseDelta(
        line: String,
        cumulative: StringBuilder,
    ): String? {
        if (!line.startsWith(SSE_DATA_PREFIX)) return null
        val payload = line.removePrefix(SSE_DATA_PREFIX).trim()
        if (payload.isEmpty() || payload == SSE_DONE_SENTINEL) return null
        val delta =
            json
                .decodeFromString<ChatCompletionChunk>(payload)
                .choices
                .firstOrNull()
                ?.delta
                ?.content
                .orEmpty()
        cumulative.append(delta)
        return delta
    }

    private companion object {
        const val SSE_DATA_PREFIX = "data:"
        const val SSE_DONE_SENTINEL = "[DONE]"
    }
}

private const val ROLE_SYSTEM = "system"
private const val ROLE_USER = "user"

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
    )
}

internal fun applyCloudLlmRequest(
    builder: HttpRequestBuilder,
    config: CloudLlmConfig,
    prompt: String,
    instructions: String,
) {
    builder.headers.append(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
    builder.contentType(ContentType.Application.Json)
    builder.accept(ContentType.Text.EventStream)
    builder.timeout {
        socketTimeoutMillis = config.socketTimeoutMillis
        connectTimeoutMillis = config.connectTimeoutMillis
    }
    builder.setBody(buildChatCompletionRequest(config, prompt, instructions))
}

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList(),
)

@Serializable
internal data class ChunkChoice(
    val delta: Delta? = null,
)

@Serializable
internal data class Delta(
    val content: String? = null,
)
