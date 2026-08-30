package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagChatMessage
import com.github.rodrigotimoteo.animally.llm.RagLlmEngine
import com.github.rodrigotimoteo.animally.llm.RagToolCall
import com.github.rodrigotimoteo.animally.llm.RagToolCallingEngine
import com.github.rodrigotimoteo.animally.llm.RagToolDefinition
import com.github.rodrigotimoteo.animally.llm.RagToolStreamEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
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
     * Overall request deadline. A provider may send keep-alive comments while
     * queued, so an inactivity timeout alone cannot prevent an endless wait.
     */
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
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
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 180_000L

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
@Suppress("TooManyFunctions")
class CloudRagLlmEngine(
    private val httpClient: HttpClient,
    private val configProvider: () -> CloudLlmConfig,
) : RagLlmEngine,
    RagToolCallingEngine {
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
            val config = configProvider()
            streamRequest(
                buildChatCompletionRequest(
                    config,
                    prompt,
                    instructions,
                ),
                config,
            ).collect { event ->
                if (event is RagToolStreamEvent.Text) emit(event.text)
            }
        }

    override val supportsToolCalling: Boolean = true

    override fun generateStreamingWithTools(
        messages: List<RagChatMessage>,
        tools: List<RagToolDefinition>,
    ): Flow<RagToolStreamEvent> {
        require(tools.isNotEmpty()) { "At least one tool definition is required." }
        val config = configProvider()
        return streamRequest(
            buildToolChatCompletionRequest(
                config = config,
                messages = messages,
                tools = tools,
            ),
            config,
        )
    }

    /**
     * Executes one OpenAI-compatible streaming request. Text and tool-call
     * responses share this parser so reasoning filtering and terminal handling
     * cannot drift between the normal and tool-aware paths.
     */
    private fun streamRequest(
        request: ChatCompletionRequest,
        config: CloudLlmConfig,
    ): Flow<RagToolStreamEvent> =
        // Ktor executes streaming response callbacks on the native engine
        // dispatcher. channelFlow safely bridges those emissions to collectors.
        channelFlow {
            httpClient
                .preparePost(cloudChatCompletionsUrl(config.baseUrl)) {
                    applyCloudLlmRequest(this, config, request)
                }.execute { response ->
                    ensureSuccessful(response)
                    streamResponse(response, ::send)
                }
        }

    private suspend fun ensureSuccessful(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val detail = response.bodyAsText().compactCloudError()
        val suffix = detail.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
        error("Cloud LLM request failed: HTTP ${response.status.value}$suffix")
    }

    private suspend fun streamResponse(
        response: HttpResponse,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        val state = StreamState()
        val channel = response.bodyAsChannel()
        var shouldRead = true
        while (shouldRead && !channel.isClosedForRead) {
            val line = channel.readUTF8Line()
            if (line == null) {
                shouldRead = false
            } else {
                processSseLine(line, state, emit)
                // Providers may send a valid terminal frame and keep the HTTP
                // connection alive for a usage trailer or heartbeat. Nothing
                // after completion can improve the answer, so stop consuming
                // immediately.
                shouldRead = !state.sawDone
            }
        }
        finishStream(state, emit)
    }

    private suspend fun processSseLine(
        line: String,
        state: StreamState,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        parseSseError(line)?.let { message -> error(message) }
        if (appendSseDelta(line, state.cumulative, state.thinkingFilter)?.isNotEmpty() == true) {
            emit(RagToolStreamEvent.Text(state.cumulative.toString()))
        }
        val toolDeltas = extractToolCallDeltas(line)
        if (toolDeltas.isNotEmpty()) {
            appendToolCallDeltas(state.toolCalls, toolDeltas)
        }
        if (isTerminalSseFrame(line)) state.sawDone = true
        parseFinishReason(line)?.let { state.finishReason = it }
    }

    private fun extractToolCallDeltas(line: String): List<ChatToolCall> =
        dataPayload(line)
            ?.takeUnless { it.isEmpty() || it.equals(SSE_DONE_SENTINEL, ignoreCase = true) }
            ?.let(::decodeChunk)
            ?.choices
            ?.firstOrNull()
            ?.let { choice ->
                choice.delta?.toolCalls ?: choice.message?.toolCalls.orEmpty()
            }.orEmpty()

    private suspend fun finishStream(
        state: StreamState,
        emit: suspend (RagToolStreamEvent) -> Unit,
    ) {
        state.thinkingFilter
            .finish()
            .takeIf(String::isNotEmpty)
            ?.let { tail ->
                state.cumulative.append(tail)
                emit(RagToolStreamEvent.Text(state.cumulative.toString()))
            }
        val failure =
            validateStreamEnd(
                sawDone = state.sawDone,
                finishReason = state.finishReason,
                contentLength = state.cumulative.length,
            )
        if (failure != null) error(failure)
        val calls =
            state.toolCalls
                .toList()
                .sortedBy { (index, _) -> index }
                .mapIndexedNotNull { index, (_, call) -> call.toRagToolCall(index) }
        if (calls.isNotEmpty()) emit(RagToolStreamEvent.ToolCalls(calls))
    }

    private class StreamState {
        val cumulative = StringBuilder()
        val thinkingFilter = ThinkingBlockFilter()
        val toolCalls = linkedMapOf<Int, MutableCloudToolCall>()
        var sawDone = false
        var finishReason: String? = null
    }

    private fun appendToolCallDeltas(
        accumulator: MutableMap<Int, MutableCloudToolCall>,
        deltas: List<ChatToolCall>,
    ) {
        deltas.forEachIndexed { fallbackIndex, delta ->
            val index = delta.index ?: fallbackIndex
            val current = accumulator.getOrPut(index) { MutableCloudToolCall() }
            delta.id?.takeIf(String::isNotBlank)?.let { current.id = it }
            delta.function?.name?.let { current.name.append(it) }
            delta.function?.arguments?.let { current.arguments.append(it) }
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
                ?.visibleContent()
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
            else -> decodeChunk(payload)?.isTerminal(isBareJsonBody = normalized.startsWith("{")) == true
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

    private fun dataPayload(line: String): String? {
        val normalized = line.trimStart()
        return when {
            normalized.startsWith(SSE_DATA_PREFIX, ignoreCase = true) -> normalized.substringAfter(':').trim()
            // Some OpenAI-compatible gateways ignore `stream: true` and return
            // one ordinary chat-completion JSON body. Treat it as a one-frame
            // stream instead of reporting a misleading EOF interruption.
            normalized.startsWith("{") && normalized.endsWith("}") -> normalized
            normalized.equals(SSE_DONE_SENTINEL, ignoreCase = true) -> normalized
            else -> null
        }
    }

    private fun ChunkChoice.visibleContent(): String {
        val candidates =
            listOfNotNull(
                delta?.content,
                message?.content,
                text,
            ).map { element -> element.textContent() }.filter(String::isNotEmpty).distinct()
        if (candidates.isEmpty()) return ""
        if (candidates.size == 1) return candidates.single()
        val longest = candidates.maxBy { it.length }
        return if (candidates.any { it != longest && longest.contains(it) }) {
            longest
        } else {
            candidates.joinToString(separator = "")
        }
    }

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
                    (this["text"] ?: this["content"] ?: this["value"])?.textContent().orEmpty()
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
        val THINKING_CONTENT_TYPES =
            setOf(
                "analysis",
                "analysis_content",
                "reasoning",
                "reasoning_content",
                "reasoning_details",
                "reasoning_detail",
                "reasoning_summary",
                "redacted_reasoning",
                "redacted_thinking",
                "thinking",
                "thinking_block",
                "thinking_details",
                "thought",
                "thoughts",
                "thought_summary",
            )
    }
}

private val TERMINAL_EVENT_NAMES =
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

private val TERMINAL_STATUS_NAMES = setOf("complete", "completed", "done", "finished")

private fun ChatCompletionChunk.isTerminal(isBareJsonBody: Boolean): Boolean {
    val choice = choices.firstOrNull()
    return cost != null ||
        choice?.message != null ||
        (isBareJsonBody && choice?.text != null) ||
        done?.isTrueFlag() == true ||
        status?.lowercase() in TERMINAL_STATUS_NAMES ||
        event?.lowercase() in TERMINAL_EVENT_NAMES ||
        type?.lowercase() in TERMINAL_EVENT_NAMES ||
        finishReason != null ||
        choice?.finishReason != null
}

private fun JsonElement.isTrueFlag(): Boolean =
    when (this) {
        is JsonPrimitive -> contentOrNull.equals("true", ignoreCase = true)
        else -> false
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
        while (pending.isNotEmpty()) {
            val marker = findMarker(pending, activeMarkers())
            if (marker != null) {
                consumeMarker(marker, visible)
            } else {
                visible.append(drainUnmarked(final))
                break
            }
        }
        return visible.toString()
    }

    private fun consumeMarker(
        marker: ThinkingMarkerMatch,
        visible: StringBuilder,
    ) {
        if (!inThinking) visible.append(pending, 0, marker.index)
        pending = pending.drop(marker.index + marker.token.length)
        inThinking =
            when {
                marker.entersThinking -> true
                marker.exitsThinking -> false
                else -> inThinking
            }
    }

    private fun drainUnmarked(final: Boolean): String {
        if (inThinking) {
            pending = if (final) "" else pending.takeLast(markerPrefixLength(pending))
            return ""
        }
        val keep = if (final) 0 else markerPrefixLength(pending)
        if (keep == 0) {
            return pending.also { pending = "" }
        }
        val visible = pending.dropLast(keep)
        pending = pending.takeLast(keep)
        return visible
    }

    private fun activeMarkers(): List<ThinkingMarker> = if (inThinking) CLOSE_MARKERS + STRIP_MARKERS else ALL_MARKERS

    private fun findMarker(
        text: String,
        markers: List<ThinkingMarker>,
    ): ThinkingMarkerMatch? {
        val lowered = text.lowercase()
        return markers
            .mapNotNull { marker ->
                lowered.indexOf(marker.token.lowercase()).takeIf { it >= 0 }?.let { index ->
                    ThinkingMarkerMatch(index, marker.token, marker.entersThinking, marker.exitsThinking)
                }
            }.minWithOrNull(compareBy({ it.index }, { -it.token.length }))
    }

    private fun markerPrefixLength(text: String): Int {
        val lowered = text.lowercase()
        return ALL_MARKERS
            .maxOfOrNull { marker ->
                (1..minOf(lowered.length, marker.token.length - 1))
                    .filter { length -> lowered.endsWith(marker.token.lowercase().take(length)) }
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
                "<|begin_of_analysis|>",
                "<|start|>analysis",
                "<|start|>reasoning",
                "<|start|>thinking",
                "<|thought|>",
                "[THINK]",
                "[THOUGHT]",
                "<|channel|>analysis<|message|>",
                "<|channel|>reasoning<|message|>",
                "<|channel|>analysis",
                "<|channel|>reasoning",
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
                "<|end_of_analysis|>",
                "[/THINK]",
                "[/THOUGHT]",
                "<|end_analysis|>",
                "<|end_reasoning|>",
                "<|end|>",
                "<|channel|>final<|message|>",
                "<|channel|>commentary<|message|>",
            ).map { ThinkingMarker(it, exitsThinking = true) }
        val STRIP_MARKERS =
            listOf(
                "<|start|>assistant",
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
 * termination is legitimate ([DONE], or an allow-listed finish_reason such as `stop`).
 * `length` always means the provider stopped before completing the answer. Partial
 * visible text is retained by the caller and paired with its retry affordance.
 */
internal fun validateStreamEnd(
    sawDone: Boolean,
    finishReason: String?,
    contentLength: Int,
): String? =
    when (finishReason?.trim()?.lowercase()) {
        FINISH_LENGTH ->
            if (contentLength == 0) {
                "Cloud model spent its entire token budget on reasoning and returned no answer"
            } else {
                "Cloud model reached its output limit before completing the answer"
            }
        FINISH_CONTENT_FILTER, FINISH_ERROR, FINISH_FAILED, FINISH_CANCELLED, FINISH_CANCELED ->
            "Cloud model ended the answer with finish reason '${finishReason.trim()}'."
        in FINISH_SUCCESS_REASONS -> null
        null -> if (sawDone) null else "Cloud LLM stream ended before completion"
        else -> "Cloud model ended with an unrecognized finish reason '${finishReason.trim()}'."
    }

private const val FINISH_LENGTH = "length"
private const val FINISH_CONTENT_FILTER = "content_filter"
private const val FINISH_ERROR = "error"
private const val FINISH_FAILED = "failed"
private const val FINISH_CANCELLED = "cancelled"
private const val FINISH_CANCELED = "canceled"

private val FINISH_SUCCESS_REASONS =
    setOf(
        "stop",
        "tool_calls",
        "function_call",
        "end_turn",
        "completed",
        "complete",
        "done",
        "eos",
    )

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
    if (config.apiKey.isNotBlank()) {
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
    if (config.apiKey.isNotBlank()) {
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

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
    /** Null is omitted by the request Json configuration for cloud providers. */
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
    /** Non-streaming-compatible gateways put the completed answer here. */
    val message: CompletionMessage? = null,
    /** Legacy text-completion gateways sometimes use `choices[].text`. */
    val text: JsonElement? = null,
    /** Terminal marker (`stop`, `length`, ...); absent on every non-final chunk. */
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class CompletionMessage(
    val content: JsonElement? = null,
    @SerialName("tool_calls") val toolCalls: List<ChatToolCall>? = null,
    @SerialName("reasoning_content") val reasoningContent: JsonElement? = null,
    @SerialName("reasoning_details") val reasoningDetails: JsonElement? = null,
    val reasoning: JsonElement? = null,
    val thinking: JsonElement? = null,
    val analysis: JsonElement? = null,
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

private const val TOOL_TYPE_FUNCTION = "function"
private const val TOOL_CHOICE_AUTO = "auto"

private data class MutableCloudToolCall(
    var id: String = "",
    val name: StringBuilder = StringBuilder(),
    val arguments: StringBuilder = StringBuilder(),
) {
    fun toRagToolCall(index: Int): RagToolCall? {
        val toolName = name.toString().trim()
        if (toolName.isEmpty()) return null
        val argumentText = arguments.toString().trim().ifBlank { "{}" }
        if (runCatching { TOOL_ARGUMENTS_JSON.parseToJsonElement(argumentText) as? JsonObject }.getOrNull() == null) {
            return null
        }
        return RagToolCall(
            id = id.ifBlank { "tool_call_$index" },
            name = toolName,
            arguments = argumentText,
        )
    }
}

private val TOOL_ARGUMENTS_JSON =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
