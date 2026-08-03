package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Server verdict for a push batch.
 *
 * Each pushed record lands in exactly one list: [accepted] carries the
 * server-assigned [SyncAccepted.serverId] the device must persist, [rejected]
 * records must be retried or surfaced to the user.
 */
@Serializable
data class SyncPushResponse(
    val accepted: List<SyncAccepted>,
    val rejected: List<SyncRejected>,
    val serverTimestamp: Instant,
)

/**
 * A pushed record the server accepted and stored.
 */
@Serializable
data class SyncAccepted(
    val type: String,
    val clientId: Long,
    val serverId: String,
    val updatedAt: Instant,
)

/**
 * A pushed record the server refused (e.g. stale overwrite attempt).
 */
@Serializable
data class SyncRejected(
    val type: String,
    val clientId: Long,
    val reason: String,
)
