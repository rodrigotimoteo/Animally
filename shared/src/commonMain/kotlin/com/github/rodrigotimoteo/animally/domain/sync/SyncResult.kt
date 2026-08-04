package com.github.rodrigotimoteo.animally.domain.sync

import kotlin.time.Instant

/**
 * Outcome of one [SyncEngine.sync] run.
 *
 * @property success `true` when the whole cycle completed; `false` when a step
 *   threw, in which case the last-synced marker was not advanced.
 * @property pushedCount local records the server accepted.
 * @property pulledCount remote records applied to the local database.
 * @property rejectedCount local records the server refused (e.g. stale).
 * @property deferredCount local records skipped because a parent server id
 *   could not be resolved; they are retried on the next sync.
 * @property serverTimestamp the server clock reading from the pull response.
 * @property errorMessage failure reason, present only when [success] is false.
 */
data class SyncResult(
    val success: Boolean,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val rejectedCount: Int = 0,
    val deferredCount: Int = 0,
    val serverTimestamp: Instant? = null,
    val errorMessage: String? = null,
) {
    companion object {
        fun success(
            pushedCount: Int,
            pulledCount: Int,
            rejectedCount: Int,
            deferredCount: Int,
            serverTimestamp: Instant,
        ): SyncResult =
            SyncResult(
                success = true,
                pushedCount = pushedCount,
                pulledCount = pulledCount,
                rejectedCount = rejectedCount,
                deferredCount = deferredCount,
                serverTimestamp = serverTimestamp,
            )

        fun failure(errorMessage: String): SyncResult =
            SyncResult(
                success = false,
                errorMessage = errorMessage,
            )
    }
}
