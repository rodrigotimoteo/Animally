package com.github.rodrigotimoteo.animally.domain.assistant.model

import com.github.rodrigotimoteo.animally.domain.vetreference.model.VeterinaryWebSource
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Encodes web-reference cards in the local history row and backup payload. */
internal object AssistantWebSourcesCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    private val serializer = ListSerializer(VeterinaryWebSource.serializer())

    fun encode(sources: List<VeterinaryWebSource>): String = json.encodeToString(serializer, sources)

    fun decode(value: String?): List<VeterinaryWebSource> =
        value
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            ?.filter { it.hasTrustedUrl() }
            .orEmpty()
}
