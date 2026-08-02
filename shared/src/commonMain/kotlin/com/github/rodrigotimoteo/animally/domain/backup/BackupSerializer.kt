package com.github.rodrigotimoteo.animally.domain.backup

import kotlinx.serialization.json.Json

/**
 * Serializes and deserializes [BackupPayload] to and from pretty-printed JSON.
 */
object BackupSerializer {
    private val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    /**
     * Encodes [payload] to a pretty-printed JSON document.
     */
    fun encode(payload: BackupPayload): String = json.encodeToString(BackupPayload.serializer(), payload)

    /**
     * Decodes [content] and rejects payloads whose [BackupPayload.schemaVersion]
     * does not match [BACKUP_SCHEMA_VERSION].
     */
    fun decode(content: String): BackupPayload {
        val payload = json.decodeFromString(BackupPayload.serializer(), content)
        check(payload.schemaVersion == BACKUP_SCHEMA_VERSION) {
            "Unsupported backup schema version ${payload.schemaVersion}, expected $BACKUP_SCHEMA_VERSION"
        }
        return payload
    }
}
