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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
 * caller's job ([FmFirstRagLlmEngine]). A stream that ends without a recognized
 * terminal frame is treated as interrupted (not silently truncated) so callers see
 * the typed failure instead of a confident half-answer.
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
                    val thinkingFilter = ThinkingBlockFilter()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        parseSseError(line)?.let { message -> error(message) }
                        if (appendSseDelta(line, cumulative, thinkingFilter)?.isNotEmpty() == true) {
                            emit(cumulative.toString())
                        }
                        if (isTerminalSseFrame(line)) sawDone = true
                        parseFinishReason(line)?.let { finishReason = it }
                    }
                    thinkingFilter
                        .finish()
                        .takeIf(String::isNotEmpty)
                        ?.let { tail ->
                            cumulative.append(tail)
                            emit(cumulative.toString())
                        }
                    // Terminal-state validation: a connection that closes without
                    // a recognized completion frame or finish_reason dropped the
                    // answer mid-flight - surface that instead of ending the flow
                    // like a complete reply.
                    validateStreamEnd(
                        sawDone = sawDone,
                        finishReason = finishReason,
                        contentLength = cumulative.length,
                    )?.let { message -> error(message) }
                }
        }

    /**
     * Parses one SSE line; returns the content delta it carries, or null for
     * non-data lines / keep-alive comments / terminal markers /
     * anything with no visible content (reasoning-only deltas, usage tails,
     * malformed payloads — all inert, never fatal). Internal so contract tests
     * drive the exact production decode path.
     */
    internal fun appendSseDelta(
        line: String,
        cumulative: StringBuilder,
        thinkingFilter: ThinkingBlockFilter = ThinkingBlockFilter(),
    ): String? {
        val payload = dataPayload(line)
        // Null means "nothing to append": non-data lines, the [DONE] sentinel,
        // malformed payloads, and content-free frames (reasoning-only deltas,
        // usage tails) are all inert. Only a real content delta is appended.
        val delta =
            payload
                ?.takeUnless { it.isEmpty() || it.equals(SSE_DONE_SENTINEL, ignoreCase = true) }
                ?.let(::decodeChunk)
                ?.choices
                ?.firstOrNull()
                ?.delta
                ?.content
                ?.textContent()
                .orEmpty()
        val visibleDelta = delta.takeUnless(String::isEmpty)?.let(thinkingFilter::append).orEmpty()
        visibleDelta.takeIf(String::isNotEmpty)?.let(cumulative::append)
        return visibleDelta.takeIf(String::isNotEmpty)
    }

    /**
     * True for a protocol completion marker. OpenCode Go can finish a valid
     * chat-completions stream with `{"choices":[],"cost":"0"}` instead of
     * sending a finish reason or `[DONE]`; that frame is terminal, not a usage
     * chunk that should trigger the generic interruption footer.
     */
    internal fun isTerminalSseFrame(line: String): Boolean {
        val normalized = line.trim()
        val payload = dataPayload(normalized)
        return when {
            eventName(normalized) in TERMINAL_EVENT_NAMES -> true
            payload?.equals(SSE_DONE_SENTINEL, ignoreCase = true) == true -> true
            payload == null || !payload.startsWith("{") -> false
            else -> {
                val chunk = decodeChunk(payload)
                if (chunk == null) {
                    false
                } else {
                    chunk.cost != null ||
                        chunk.done?.isTrueFlag() == true ||
                        chunk.status?.lowercase() in TERMINAL_STATUS_NAMES ||
                        chunk.event?.lowercase() in TERMINAL_EVENT_NAMES ||
                        chunk.type?.lowercase() in TERMINAL_EVENT_NAMES ||
                        chunk.finishReason != null ||
                        chunk.choices.firstOrNull()?.finishReason != null
                }
            }
        }
    }

    /** Extracts `choices[0].finish_reason` from a data line, or null. */
    private fun parseFinishReason(line: String): String? =
        dataPayload(line)
            ?.takeIf { it.startsWith("{") }
            ?.let(::decodeChunk)
            ?.let { chunk -> chunk.choices.firstOrNull()?.finishReason ?: chunk.finishReason }

    /** Tolerant frame decode: malformed payloads yield null instead of throwing. */
    private fun decodeChunk(payload: String): ChatCompletionChunk? =
        runCatching {
            json.decodeFromString<ChatCompletionChunk>(payload)
        }.getOrNull()

    private fun dataPayload(line: String): String? =
        line
            .trimStart()
            .takeIf { it.startsWith(SSE_DATA_PREFIX, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()

    /** Converts string or structured text content into one provider-neutral string. */
    private fun JsonElement.textContent(): String =
        when (this) {
            is JsonPrimitive -> contentOrNull.orEmpty()
            is JsonArray -> joinToString(separator = "") { element -> element.textContent() }
            is JsonObject -> {
                val type = (this["type"] as? JsonPrimitive)?.contentOrNull?.lowercase()
                if (type in THINKING_CONTENT_TYPES) {
                    ""
                } else {
                    (this["text"] ?: this["content"])?.textContent().orEmpty()
                }
            }
        }

    /** Extracts provider error frames sent inside an otherwise successful SSE response. */
    private fun parseSseError(line: String): String? {
        val normalized = line.trim()
        val payload = dataPayload(normalized)
        return when {
            eventName(normalized) == SSE_ERROR_EVENT -> STREAM_ERROR_MESSAGE
            payload == null || !payload.startsWith("{") -> null
            else -> {
                val chunk = decodeChunk(payload)
                if (chunk?.type?.lowercase() != SSE_ERROR_EVENT && chunk?.error == null) {
                    null
                } else {
                    chunk.error
                        ?.errorMessage()
                        ?.takeIf(String::isNotBlank)
                        ?.let { "$STREAM_ERROR_MESSAGE: ${it.take(MAX_ERROR_DETAIL_CHARS)}" }
                        ?: STREAM_ERROR_MESSAGE
                }
            }
        }
    }

    private fun JsonElement.errorMessage(): String? =
        when (this) {
            is JsonPrimitive -> contentOrNull
            is JsonObject ->
                sequenceOf("message", "detail", "error")
                    .mapNotNull { key -> this[key]?.errorMessage() }
                    .firstOrNull()
            is JsonArray -> null
        }

    private fun JsonElement.isTrueFlag(): Boolean =
        when (this) {
            is JsonPrimitive -> contentOrNull.equals("true", ignoreCase = true)
            else -> false
        }

    private fun eventName(line: String): String? =
        line
            .takeIf { it.startsWith(SSE_EVENT_PREFIX, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.lowercase()

    private fun String.compactCloudError(): String = replace(Regex("\\s+"), " ").trim().take(MAX_ERROR_DETAIL_CHARS)

    private companion object {
        const val SSE_DATA_PREFIX = "data:"
        const val SSE_EVENT_PREFIX = "event:"
        const val SSE_ERROR_EVENT = "error"
        const val SSE_DONE_SENTINEL = "[DONE]"
        const val STREAM_ERROR_MESSAGE = "Cloud LLM stream reported an error"
        const val MAX_ERROR_DETAIL_CHARS = 240
        val TERMINAL_EVENT_NAMES =
            setOf(
                "done",
                "complete",
                "completed",
                "finish",
                "finished",
                "end",
                "message_stop",
                "message_end",
                "response.completed",
                "response.done",
                "completion",
                "stream_end",
            )
        val TERMINAL_STATUS_NAMES = setOf("complete", "completed", "done", "finished")
        val THINKING_CONTENT_TYPES =
            setOf(
                "analysis",
                "analysis_content",
                "reasoning",
                "reasoning_content",
                "reasoning_details",
                "redacted_reasoning",
                "redacted_thinking",
                "thinking",
                "thinking_block",
                "thought",
                "thoughts",
            )
    }
}

/**
 * Removes common inline reasoning markers without leaking tags split across
 * network chunks. Structured reasoning fields are ignored before this filter;
 * this handles providers that put the same content inside `<think>...</think>`
 * (or equivalent) in the normal content field.
 */
internal class ThinkingBlockFilter {
    private var pending = ""
    private var inThinking = false

    /** Adds one raw content delta and returns only newly visible answer text. */
    fun append(delta: String): String {
        if (delta.isEmpty()) return ""
        pending += delta
        return drain(final = false)
    }

    /** Flushes visible text held back while checking for a tag prefix. */
    fun finish(): String = drain(final = true)

    private fun drain(final: Boolean): String {
        val visible = StringBuilder()
        var draining = true
        while (pending.isNotEmpty() && draining) {
            val marker = findMarker(pending, activeMarkers())
            if (marker != null) {
                if (!inThinking) visible.append(pending, 0, marker.index)
                pending = pending.drop(marker.index + marker.token.length)
                if (marker.entersThinking) inThinking = true
                if (marker.exitsThinking) inThinking = false
            } else if (inThinking) {
                pending = if (final) "" else pending.takeLast(markerPrefixLength(pending))
            } else {
                val keep = if (final) 0 else markerPrefixLength(pending)
                if (keep > 0) {
                    visible.append(pending, 0, pending.length - keep)
                    pending = pending.takeLast(keep)
                } else {
                    visible.append(pending)
                    pending = ""
                }
            }
            draining = marker != null && pending.isNotEmpty()
        }
        return visible.toString()
    }

    private fun activeMarkers(): List<ThinkingMarker> = if (inThinking) CLOSE_MARKERS + STRIP_MARKERS else ALL_MARKERS

    private fun findMarker(
        text: String,
        markers: List<ThinkingMarker>,
    ): ThinkingMarkerMatch? {
        val lowered = text.lowercase()
        return markers
            .mapNotNull { marker ->
                lowered.indexOf(marker.token).takeIf { it >= 0 }?.let { index ->
                    ThinkingMarkerMatch(index, marker.token, marker.entersThinking, marker.exitsThinking)
                }
            }.minWithOrNull(compareBy({ it.index }, { -it.token.length }))
    }

    private fun markerPrefixLength(text: String): Int {
        val lowered = text.lowercase()
        return ALL_MARKERS
            .maxOfOrNull { marker ->
                (1..minOf(lowered.length, marker.token.length - 1))
                    .filter { length -> lowered.endsWith(marker.token.take(length)) }
                    .maxOrNull()
                    ?: 0
            } ?: 0
    }

    private data class ThinkingMarker(
        val token: String,
        val entersThinking: Boolean = false,
        val exitsThinking: Boolean = false,
    )

    private data class ThinkingMarkerMatch(
        val index: Int,
        val token: String,
        val entersThinking: Boolean,
        val exitsThinking: Boolean,
    )

    private companion object {
        val OPEN_MARKERS =
            listOf(
                "<think>",
                "<thinking>",
                "<analysis>",
                "<reasoning>",
                "<|thinking|>",
                "<|analysis|>",
                "<|reasoning|>",
                "<|begin_of_thought|>",
                "<|thought|>",
                "<|channel|>analysis<|message|>",
                "<|channel|>reasoning<|message|>",
            ).map { ThinkingMarker(it, entersThinking = true) }
        val CLOSE_MARKERS =
            listOf(
                "</think>",
                "</thinking>",
                "</analysis>",
                "</reasoning>",
                "<|end_thinking|>",
                "<|end_thought|>",
                "<|end_of_thought|>",
                "<|end_analysis|>",
                "<|end_reasoning|>",
                "<|end|>",
                "<|channel|>final<|message|>",
                "<|channel|>commentary<|message|>",
            ).map { ThinkingMarker(it, exitsThinking = true) }
        val STRIP_MARKERS =
            listOf(
                "<|start|>assistant",
                "<|start|>analysis",
                "<|start|>final",
                "<|message|>",
                "<|channel|>final",
                "<|channel|>commentary",
            ).map { ThinkingMarker(it) }
        val ALL_MARKERS = (OPEN_MARKERS + CLOSE_MARKERS + STRIP_MARKERS).sortedByDescending { it.token.length }
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
    val event: String? = null,
    val type: String? = null,
    val status: String? = null,
    val done: JsonElement? = null,
    /** Some compatible APIs put the terminal reason at the top level. */
    @SerialName("finish_reason") val finishReason: String? = null,
    /** Some providers send an in-band error frame with HTTP 200. */
    val error: JsonElement? = null,
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
    /** Chat-completions providers use a string; some compatible APIs use blocks. */
    val content: JsonElement? = null,
    /**
     * Reasoning models stream hidden chain-of-thought here BEFORE/AFTER content
     * deltas. Never rendered; declared so the shape is documented and greppable
     * (unknown keys are ignored anyway).
     */
    @SerialName("reasoning_content") val reasoningContent: JsonElement? = null,
    @SerialName("reasoning_details") val reasoningDetails: JsonElement? = null,
    val reasoning: JsonElement? = null,
    val thinking: JsonElement? = null,
    val analysis: JsonElement? = null,
)
