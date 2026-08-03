package com.github.rodrigotimoteo.animally.domain.sync

import kotlin.time.Instant

/**
 * Contract of the sync server.
 *
 * [pull] fetches every record changed after [since]; the returned
 * [SyncPullResponse.serverTimestamp] becomes the next `since`. [push] submits
 * local changes and returns per-record accept/reject verdicts.
 */
interface SyncApi {
    suspend fun pull(since: Instant): SyncPullResponse

    suspend fun push(request: SyncPushRequest): SyncPushResponse
}
