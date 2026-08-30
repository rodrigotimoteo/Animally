package com.github.rodrigotimoteo.animally.llm.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * SSE parsing delegate extracted from CloudRagLlmEngine.
 * Owns all pure helpers for OpenAI-compatible streaming: payload extraction,
 * terminal detection, visible-content filtering and thinking-block handling.
 */
internal object CloudStreamParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun dataPayload(line: String): String? {
        val normalized = line.trimStart()
        return when {
            normalized.startsWith(SSE_DATA_PREFIX, ignoreCase = true) -> normalized.substringAfter(':').trim()
            normalized.startsWith("{") && normalized.endsWith("}") -> normalized
            normalized.equals(SSE_DONE_SENTINEL, ignoreCase = true) -> normalized
            else -> null
        }
    }

    fun eventName(line: String): String? =
        line
            .takeIf { it.startsWith(SSE_EVENT_PREFIX, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.lowercase()

    fun decodeChunk(payload: String): ChatCompletionChunk? =
        runCatching {
            json.decodeFromString<ChatCompletionChunk>(payload)
        }.getOrNull()

    fun parseFinishReason(line: String): String? =
        dataPayload(line)
            ?.takeIf { it.startsWith("{") }
            ?.let(::decodeChunk)
            ?.let { chunk -> chunk.choices.firstOrNull()?.finishReason ?: chunk.finishReason }

    fun parseSseError(line: String): String? {
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

    fun isTerminalSseFrame(
        line: String,
        hasActivity: Boolean = false,
    ): Boolean {
        val normalized = line.trim()
        val payload = dataPayload(normalized)
        return when {
            eventName(normalized) in TERMINAL_EVENT_NAMES -> true
            payload?.equals(SSE_DONE_SENTINEL, ignoreCase = true) == true -> true
            payload == null || !payload.startsWith("{") -> false
            else ->
                decodeChunk(payload)?.isTerminal(
                    isBareJsonBody = normalized.startsWith("{"),
                    hasActivity = hasActivity,
                ) == true
        }
    }

    fun appendSseDelta(
        line: String,
        cumulative: StringBuilder,
        thinkingFilter: ThinkingBlockFilter = ThinkingBlockFilter(),
    ): String? {
        val payload = dataPayload(line)
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

    private fun JsonElement.errorMessage(): String? =
        when (this) {
            is JsonPrimitive -> contentOrNull
            is JsonObject ->
                sequenceOf("message", "detail", "error")
                    .mapNotNull { key -> this[key]?.errorMessage() }
                    .firstOrNull()
            is JsonArray -> null
        }

    const val SSE_DATA_PREFIX = "data:"
    const val SSE_EVENT_PREFIX = "event:"
    const val SSE_ERROR_EVENT = "error"
    const val SSE_DONE_SENTINEL = "[DONE]"
    const val STREAM_ERROR_MESSAGE = "Cloud LLM stream reported an error"
    const val MAX_CONSECUTIVE_MALFORMED_DATA_FRAMES = 2
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
}

private fun ChatCompletionChunk.isTerminal(
    isBareJsonBody: Boolean,
    hasActivity: Boolean,
): Boolean {
    val choice = choices.firstOrNull()
    return cost != null ||
        (usage != null && hasActivity) ||
        choice?.message != null ||
        (isBareJsonBody && choice?.text != null) ||
        done?.isTrueFlag() == true ||
        status?.lowercase() in CloudStreamParser.TERMINAL_STATUS_NAMES ||
        event?.lowercase() in CloudStreamParser.TERMINAL_EVENT_NAMES ||
        type?.lowercase() in CloudStreamParser.TERMINAL_EVENT_NAMES ||
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
 * network chunks. Extracted from CloudRagLlmEngine to keep the engine thin.
 */
internal class ThinkingBlockFilter {
    private var pending = ""
    private var inThinking = false

    fun append(delta: String): String {
        if (delta.isEmpty()) return ""
        pending += delta
        return drain(final = false)
    }

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
        val markerPrefix = markerPrefixLength(pending)
        if (final && markerPrefix > 0) {
            val visible = pending.dropLast(markerPrefix)
            pending = ""
            return visible
        }
        val keep = if (final) 0 else markerPrefix
        val visible = if (keep == 0) pending else pending.dropLast(keep)
        pending = if (keep == 0) "" else pending.takeLast(keep)
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
 * Terminal-state check shared by the stream loop and contract tests.
 */
internal fun validateStreamEnd(
    sawDone: Boolean,
    finishReason: String?,
    contentLength: Int,
    hasToolCallActivity: Boolean = false,
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
        in FINISH_SUCCESS_REASONS ->
            if (contentLength == 0 && !hasToolCallActivity) {
                "Cloud model returned no visible answer"
            } else {
                null
            }
        null ->
            when {
                !sawDone -> "Cloud LLM stream ended before completion"
                contentLength == 0 && !hasToolCallActivity -> "Cloud model returned no visible answer"
                else -> null
            }
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
