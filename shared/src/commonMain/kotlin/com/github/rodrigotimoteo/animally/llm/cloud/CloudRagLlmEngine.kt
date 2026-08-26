package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Configuration for the OpenAI-compatible chat-completions endpoint used by
 * [CloudRagLlmEngine]. The API key is supplied separately (secure storage) so this
 * data class stays loggable.
 */
data class CloudLlmConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val apiKey: String,
    /**
     * Socket INACTIVITY timeout. Reasoning models can stay silent well past 30s
     * while thinking before (and between) visible tokens, so this must be far
     * more generous than a page-load budget - a mid-thought kill surfaces to the
     * user as "Response cut short".
     */
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MILLIS,
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    /**
     * Optional completion budget for local OpenAI-compatible runtimes.
     * Cloud providers choose their own supported limit, so this is omitted there.
     */
    val maxTokens: Int? = null,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1/chat/completions"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 120_000L
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L

        /** Default output budget for local OpenAI-compatible runtimes. */
        const val DEFAULT_MAX_TOKENS = 2048
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
 * caller's job ([FmFirstRagLlmEngine]). A stream that ends WITHOUT the `[DONE]`
 * sentinel or a terminal `finish_reason` is treated as interrupted (not silently
 * truncated) so callers see the typed failure instead of a confident half-answer.
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
            var sawDone = false
            var finishReason: String? = null
            httpClient
                .preparePost(cloudChatCompletionsUrl(config.baseUrl)) {
                    applyCloudLlmRequest(this, config, prompt, instructions)
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        val detail = response.bodyAsText().compactCloudError()
                        val suffix = detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
                        error("Cloud LLM request failed: HTTP ${response.status.value}$suffix")
                    }
                    val channel = response.bodyAsChannel()
                    val cumulative = StringBuilder()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (appendSseDelta(line, cumulative)?.isNotEmpty() == true) {
                            emit(cumulative.toString())
                        }
                        if (isTerminalSseFrame(line)) sawDone = true
                        parseFinishReason(line)?.let { finishReason = it }
                    }
                    // Terminal-state validation: a connection that closes without
                    // [DONE] nor a finish_reason dropped the answer mid-flight -
                    // surface that instead of ending the flow like a complete reply.
                    validateStreamEnd(
                        sawDone = sawDone,
                        finishReason = finishReason,
                        contentLength = cumulative.length,
                    )?.let { message -> error(message) }
                }
        }

    /**
     * Parses one SSE line; returns the content delta it carries, or null for
     * non-data lines / keep-alive comments / the terminal `[DONE]` sentinel /
     * anything with no visible content (reasoning-only deltas, usage tails,
     * malformed payloads — all inert, never fatal). Internal so contract tests
     * drive the exact production decode path.
     */
    internal fun appendSseDelta(
        line: String,
        cumulative: StringBuilder,
    ): String? {
        val payload =
            line
                .takeIf { it.startsWith(SSE_DATA_PREFIX) }
                ?.removePrefix(SSE_DATA_PREFIX)
                ?.trim()
        if (payload.isNullOrEmpty() || payload == SSE_DONE_SENTINEL) return null
        // Null means "nothing to append": non-data lines, the [DONE] sentinel,
        // malformed payloads, and content-free frames (reasoning-only deltas,
        // usage tails) are all inert. Only a real content delta is appended.
        val delta =
            decodeChunk(payload)
                ?.choices
                ?.firstOrNull()
                ?.delta
                ?.content
                ?: return null
        cumulative.append(delta)
        return delta
    }

    /**
     * True for a protocol completion marker. OpenCode Go can finish a valid
     * chat-completions stream with `{"choices":[],"cost":"0"}` instead of
     * sending a finish reason or `[DONE]`; that frame is terminal, not a usage
     * chunk that should trigger the generic interruption footer.
     */
    internal fun isTerminalSseFrame(line: String): Boolean {
        val payload = line.removePrefix(SSE_DATA_PREFIX).trim()
        return when {
            payload == SSE_DONE_SENTINEL -> true
            !line.startsWith(SSE_DATA_PREFIX) || !payload.startsWith("{") -> false
            else -> {
                val chunk = decodeChunk(payload)
                chunk?.cost != null || chunk?.choices?.firstOrNull()?.finishReason != null
            }
        }
    }

    /** Extracts `choices[0].finish_reason` from a data line, or null. */
    private fun parseFinishReason(line: String): String? =
        line
            .removePrefix(SSE_DATA_PREFIX)
            .trim()
            .takeIf { it.startsWith("{") }
            ?.let(::decodeChunk)
            ?.choices
            ?.firstOrNull()
            ?.finishReason

    /** Tolerant frame decode: malformed payloads yield null instead of throwing. */
    private fun decodeChunk(payload: String): ChatCompletionChunk? =
        runCatching {
            json.decodeFromString<ChatCompletionChunk>(payload)
        }.getOrNull()

    private fun String.compactCloudError(): String = replace(Regex("\\s+"), " ").trim().take(MAX_ERROR_DETAIL_CHARS)

    private companion object {
        const val SSE_DATA_PREFIX = "data:"
        const val SSE_DONE_SENTINEL = "[DONE]"
        const val MAX_ERROR_DETAIL_CHARS = 240
    }
}

/**
 * Terminal-state check shared by the stream loop and contract tests. Returns a
 * human-readable failure message when the stream ended abnormally, null when the
 * termination is legitimate ([DONE], or an explicit finish_reason such as `stop`).
 * `length` with zero visible content means the token budget was consumed entirely
 * by reasoning - reported explicitly instead of surfacing as an empty reply.
 */
internal fun validateStreamEnd(
    sawDone: Boolean,
    finishReason: String?,
    contentLength: Int,
): String? =
    when {
        sawDone || finishReason != null ->
            if (finishReason == FINISH_LENGTH && contentLength == 0) {
                "Cloud model spent its entire token budget on reasoning and returned no answer"
            } else {
                null
            }
        else -> "Cloud LLM stream ended before completion"
    }

private const val FINISH_LENGTH = "length"

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
        maxTokens = config.maxTokens,
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
    /** Null is omitted by the request Json configuration for cloud providers. */
    @SerialName("max_tokens") val maxTokens: Int?,
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class ChatCompletionChunk(
    val choices: List<ChunkChoice> = emptyList(),
    /** OpenCode Go's non-standard terminal usage/cost trailer. */
    val cost: JsonElement? = null,
)

@Serializable
internal data class ChunkChoice(
    val delta: Delta? = null,
    /** Terminal marker (`stop`, `length`, ...); absent on every non-final chunk. */
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class Delta(
    val content: String? = null,
    /**
     * Reasoning models stream hidden chain-of-thought here BEFORE/AFTER content
     * deltas. Never rendered; declared so the shape is documented and greppable
     * (unknown keys are ignored anyway).
     */
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)
