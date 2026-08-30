package com.github.rodrigotimoteo.animally.llm.cloud

import com.github.rodrigotimoteo.animally.llm.RagToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Tool-call dispatch delegate extracted from CloudRagLlmEngine.
 * Accumulates fragmented tool-call deltas streamed via SSE and
 * materializes validated [RagToolCall] instances.
 */
internal object CloudToolDispatcher {
    internal val toolArgumentsJson =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun extractToolCallDeltas(line: String): List<ChatToolCall> =
        CloudStreamParser
            .dataPayload(line)
            ?.takeUnless { it.isEmpty() || it.equals(CloudStreamParser.SSE_DONE_SENTINEL, ignoreCase = true) }
            ?.let(CloudStreamParser::decodeChunk)
            ?.choices
            ?.firstOrNull()
            ?.let { choice ->
                choice.delta?.toolCalls ?: choice.message?.toolCalls.orEmpty()
            }.orEmpty()

    fun appendToolCallDeltas(
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

    fun toRagToolCalls(accumulator: Map<Int, MutableCloudToolCall>): List<RagToolCall> =
        accumulator
            .toList()
            .sortedBy { (index, _) -> index }
            .mapIndexedNotNull { index, (_, call) -> call.toRagToolCall(index) }
}

internal data class MutableCloudToolCall(
    var id: String = "",
    val name: StringBuilder = StringBuilder(),
    val arguments: StringBuilder = StringBuilder(),
) {
    fun toRagToolCall(index: Int): RagToolCall? {
        val toolName = name.toString().trim()
        if (toolName.isEmpty()) return null
        val argumentText = arguments.toString().trim().ifBlank { "{}" }
        val parsed =
            runCatching {
                CloudToolDispatcher.toolArgumentsJson.parseToJsonElement(argumentText) as? JsonObject
            }.getOrNull()
        if (parsed == null) {
            return null
        }
        return RagToolCall(
            id = id.ifBlank { "tool_call_$index" },
            name = toolName,
            arguments = argumentText,
        )
    }
}
