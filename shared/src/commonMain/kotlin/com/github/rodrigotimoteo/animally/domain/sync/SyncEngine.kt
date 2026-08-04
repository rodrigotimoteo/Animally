package com.github.rodrigotimoteo.animally.domain.sync

/**
 * Orchestrates one full sync cycle: push local changes to the sync server in
 * dependency order, pull remote changes back, then advance the last-synced
 * marker on success.
 */
interface SyncEngine {
    /**
     * Runs a full sync cycle.
     *
     * @return a [SyncResult] describing the outcome; `success == false` when any
     *   step threw, in which case the last-synced marker is left untouched so
     *   the next run retries the whole cycle.
     */
    suspend fun sync(): SyncResult
}
