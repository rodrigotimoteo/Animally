package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Shared JSON codec for entity payloads.
 *
 * [ignoreUnknownKeys] tolerates server-side additions; [explicitNulls] keeps
 * `null` FKs/optionals explicit on the wire so payloads stay self-describing.
 */
val SyncJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

/** Decodes [this] record's payload body with [serializer]. */
fun <T> SyncRecord.decode(serializer: KSerializer<T>): T = SyncJson.decodeFromJsonElement(serializer, payload)
