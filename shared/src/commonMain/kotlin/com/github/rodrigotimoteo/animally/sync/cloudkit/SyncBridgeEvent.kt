package com.github.rodrigotimoteo.animally.sync.cloudkit

import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import com.github.rodrigotimoteo.animally.domain.sync.handlers.SyncJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

/**
 * One record envelope exchanged with the CloudKit shim.
 *
 * [body] carries the entity DTO JSON as an opaque string; [parents] maps
 * parent FK wire names (e.g. `"ownerId"`) to the parent's CloudKit record
 * name. [activeFlag] is `0|1`; `0` marks a tombstone — tombstones are
 * exported, never filtered, so deletes propagate instead of resurrecting rows.
 */
@Serializable
data class CloudKitEnvelope(
    val recordType: String,
    val recordName: String,
    val updatedAt: Long,
    @SerialName("isActive") private val activeFlag: Int = 1,
    val parents: Map<String, String?> = emptyMap(),
    val body: String,
) {
    /** `true` when the row is live, `false` when it is a tombstone. */
    val isActive: Boolean get() = activeFlag == 1

    /**
     * Converts this envelope into the handler-facing [SyncRecord]: the CloudKit
     * record name becomes [SyncRecord.serverId], the body string is re-parsed
     * into the payload object, parents pass through unchanged.
     */
    fun toSyncRecord(): SyncRecord =
        SyncRecord(
            type = recordType,
            serverId = recordName,
            clientId = null,
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isActive = isActive,
            parentServerIds = parents,
            payload = SyncJson.parseToJsonElement(body).jsonObject,
        )

    companion object {
        /**
         * Builds an envelope from handler-produced parts; [payload] is the entity body object.
         * The parameter list mirrors every top-level CKRecord field by design.
         */
        @Suppress("LongParameterList")
        fun from(
            recordType: String,
            recordName: String,
            updatedAtMs: Long,
            isActive: Boolean,
            parents: Map<String, String>,
            payload: JsonObject,
        ): CloudKitEnvelope =
            CloudKitEnvelope(
                recordType = recordType,
                recordName = recordName,
                updatedAt = updatedAtMs,
                activeFlag = if (isActive) 1 else 0,
                parents = parents,
                body = payload.toString(),
            )
    }
}

/** Events delivered by [SyncCloudBridge.setEventHandler], parsed from JSON. */
sealed interface SyncBridgeEvent {
    /** iCloud account availability flipped. */
    data class AccountChange(
        val available: Boolean,
    ) : SyncBridgeEvent

    /** A fetch returned records to apply locally. */
    data class Imported(
        val records: List<CloudKitEnvelope>,
    ) : SyncBridgeEvent

    /** The listed record names were confirmed exported. */
    data class Exported(
        val names: List<String>,
    ) : SyncBridgeEvent

    /** The listed record names failed to export and must be retried next sync. */
    data class ExportFailed(
        val names: List<String>,
        val error: String? = null,
    ) : SyncBridgeEvent
}

internal val BridgeEventJson: Json =
    Json {
        ignoreUnknownKeys = true
    }

private val NamesSerializer = ListSerializer(String.serializer())

private val EnvelopesSerializer = ListSerializer(CloudKitEnvelope.serializer())

private fun JsonObject.primitive(key: String): String? = (this[key] as? JsonPrimitive)?.content

/**
 * Parses one shim event JSON into a [SyncBridgeEvent]; returns `null` for
 * unknown event types so forward-compatible shims never crash the engine.
 */
fun parseSyncBridgeEvent(json: String): SyncBridgeEvent? =
    runCatching {
        val obj = BridgeEventJson.parseToJsonElement(json).jsonObject
        when (obj.primitive("type")) {
            "accountChange" -> SyncBridgeEvent.AccountChange(available = obj.primitive("available") == "true")

            "imported" -> {
                val raw = obj["records"]?.toString()
                val records =
                    if (raw != null) BridgeEventJson.decodeFromString(EnvelopesSerializer, raw) else emptyList()
                SyncBridgeEvent.Imported(records = records)
            }

            "exported" -> SyncBridgeEvent.Exported(names = decodeNames(obj["names"]?.toString()))

            "exportFailed" ->
                SyncBridgeEvent.ExportFailed(
                    names = decodeNames(obj["names"]?.toString()),
                    error = obj.primitive("error"),
                )

            else -> null
        }
    }.getOrNull()

private fun decodeNames(json: String?): List<String> =
    if (json.isNullOrEmpty()) {
        emptyList()
    } else {
        BridgeEventJson.decodeFromString(NamesSerializer, json)
    }
