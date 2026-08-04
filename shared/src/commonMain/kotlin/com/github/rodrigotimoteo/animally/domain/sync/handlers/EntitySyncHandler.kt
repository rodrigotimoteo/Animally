package com.github.rodrigotimoteo.animally.domain.sync.handlers

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.domain.sync.SyncEntityHandler
import com.github.rodrigotimoteo.animally.domain.sync.SyncRecord
import kotlin.time.Instant

/**
 * Shared machinery for all [SyncEntityHandler] implementations.
 *
 * Handlers combine two access paths:
 *  - the entity's repository interface for row CRUD (the domain-facing path
 *    used by the rest of the app), and
 *  - the [database] directly for server-id bookkeeping — the generated
 *    `selectByServerId`/`setServerId` queries are not exposed by any repo
 *    interface, so handlers reach the query classes directly. This keeps the
 *    repos untouched while adding the pull-side lookup/attach primitives.
 *
 * Last-write-wins comparison lives here so every handler resolves
 * push/pull conflicts identically: remote newer OR equal wins.
 */
abstract class EntitySyncHandler(
    protected val database: AnimallyDatabase,
) : SyncEntityHandler {
    /** Last-write-wins outcome for a remote record vs a local row. */
    protected enum class Lww {
        /** Remote record is newer or equal — apply it over the local row. */
        UPDATE,

        /** Local row is newer — keep it untouched. */
        KEEP,
    }

    protected fun lwwDecision(
        record: SyncRecord,
        localUpdatedAt: Instant,
    ): Lww = if (record.updatedAt >= localUpdatedAt) Lww.UPDATE else Lww.KEEP

    /** Resolves an owner [serverId] to a local owner id, or `null` when unknown. */
    protected fun localOwnerIdFor(ownerServerId: String?): Long? =
        ownerServerId?.let {
            database.ownerQueries
                .selectByServerId(it)
                .executeAsOneOrNull()
                ?.id
        }

    /** Resolves a patient [serverId] to a local patient id, or `null` when unknown. */
    protected fun localPatientIdFor(patientServerId: String?): Long? =
        patientServerId?.let {
            database.patientQueries
                .selectByServerId(it)
                .executeAsOneOrNull()
                ?.id
        }
}
