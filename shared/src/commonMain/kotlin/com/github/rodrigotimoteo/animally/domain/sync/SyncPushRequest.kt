package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable

/**
 * Batch of local changes pushed to the sync server.
 *
 * [deviceId] identifies the producing device for server-side conflict logging;
 * [records] carries only dirty rows (new or updated since the last sync).
 */
@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val records: List<SyncRecord>,
)
