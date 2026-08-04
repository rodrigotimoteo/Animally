package com.github.rodrigotimoteo.animally.domain.sync

/**
 * Returned by [SyncEntityHandler.applyRecord] when the remote record could not
 * be applied — typically because a parent FK could not be resolved to a local
 * id (orphan protection) or the record carries no [SyncRecord.serverId]. The
 * sync engine may re-queue such records once their parents are in place.
 */
const val ENTITY_NOT_APPLIED: Long = -1L

/**
 * Per-entity bridge between the local offline-first database and the sync
 * wire format ([SyncRecord]).
 *
 * [buildRecord] serializes one local DB row into a [SyncRecord] for the push
 * direction; [applyRecord] applies a remote [SyncRecord] back into the local
 * DB for the pull direction, preserving [SyncRecord.serverId] so subsequent
 * pulls resolve the same row instead of duplicating it.
 *
 * Parent foreign keys (e.g. `ownerId`, `patientId`) travel in
 * [SyncRecord.parentServerIds], not in the payload. [applyRecord] resolves
 * them to local ids through the corresponding parent handler's
 * [localIdFor]; when a parent cannot be resolved the record is skipped
 * and [ENTITY_NOT_APPLIED] is returned.
 */
interface SyncEntityHandler {
    /** The wire entity this handler serializes. */
    val entityType: SyncEntityType

    /**
     * Serializes the local row [entityId] into a [SyncRecord] for push.
     *
     * [parentServerIds] lets the sync engine pre-resolve parent references
     * (cascade pushes). When empty, the handler computes the parent mapping
     * itself by reading the row's parent row server id.
     *
     * @throws NoSuchElementException when no row exists for [entityId].
     */
    suspend fun buildRecord(
        entityId: Long,
        parentServerIds: Map<String, String?> = emptyMap(),
    ): SyncRecord

    /**
     * Applies a remote [SyncRecord] into the local DB.
     *
     * @return the local entity id (new or existing), or [ENTITY_NOT_APPLIED]
     *   when the record was skipped (missing serverId or unresolvable parent).
     */
    suspend fun applyRecord(record: SyncRecord): Long

    /** Returns the stored server id of local row [entityId], or `null` when unsynced. */
    suspend fun serverIdOf(entityId: Long): String?

    /** Resolves [serverId] to a local row id, or `null` when unknown. */
    suspend fun localIdFor(serverId: String): Long?
}
