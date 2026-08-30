package com.github.rodrigotimoteo.animally.domain.assistant.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Encodes cited record identities in local history rows and backup payloads. */
internal object AssistantRecordSourcesCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    private val serializer = ListSerializer(AssistantRecordSource.serializer())

    fun encode(sources: List<AssistantRecordSource>): String = json.encodeToString(serializer, sources)

    fun decode(value: String?): List<AssistantRecordSource> =
        value
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()
}
