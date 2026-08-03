package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

/**
 * One syncable entity payload exchanged between the device and the sync server.
 *
 * [type] is the wire name from [SyncEntityType]. Exactly one of [serverId]
 * (server-assigned, stable across devices) or [clientId] (local DB id, assigned
 * by the producing device) is meaningful per direction: push sends [clientId]
 * for unsynced rows, pull receives [serverId]. [parentServerIds] maps local
 * parent identifiers (e.g. `"ownerId"`) to server ids so the server can
 * resolve ownership references. [payload] carries the entity body as opaque
 * JSON — this lane defines no entity-specific fields.
 */
@Serializable
data class SyncRecord(
    val type: String,
    val serverId: String? = null,
    val clientId: Long? = null,
    val updatedAt: Instant,
    val isActive: Boolean = true,
    val parentServerIds: Map<String, String?> = emptyMap(),
    val payload: JsonObject = JsonObject(emptyMap()),
)
