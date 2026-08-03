package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Server-side changes returned for a pull request.
 *
 * [records] contains every stored record with `updatedAt` strictly newer than
 * the requested `since` marker. [serverTimestamp] is the authoritative clock
 * reading the device should persist as its next `since`.
 */
@Serializable
data class SyncPullResponse(
    val records: List<SyncRecord>,
    val serverTimestamp: Instant,
)
