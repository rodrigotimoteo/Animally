package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
 * resolve ownership references. A null parent is unresolved unless its key is
 * listed in [explicitlyClearedParentKeys], which represents an intentional
 * optional-parent unlink and is local engine metadata, not wire data.
 * [payload] carries the entity body as opaque JSON — this lane defines no
 * entity-specific fields.
 */
@Serializable
data class SyncRecord(
    val type: String,
    val serverId: String? = null,
    val clientId: Long? = null,
    val updatedAt: Instant,
    val isActive: Boolean = true,
    val parentServerIds: Map<String, String?> = emptyMap(),
    @Transient val explicitlyClearedParentKeys: Set<String> = emptySet(),
    val payload: JsonObject = JsonObject(emptyMap()),
)

/** Whether the record still has a required parent that cannot be resolved. */
internal fun SyncRecord.hasUnresolvedParent(): Boolean =
    parentServerIds.any { (key, serverId) ->
        serverId == null && key !in explicitlyClearedParentKeys
    }
