package com.github.rodrigotimoteo.animally.domain.backup

/**
 * Contract for cloud backup (Phase 6, ROD-42).
 *
 * POC scope locks cloud backup as a no-op: [export] always returns
 * "not-implemented" and [import] is a successful no-op. Real iCloud/Drive
 * integration is deferred beyond the POC.
 */
interface BackupProvider {
    /**
     * Uploads the database at [databasePath] to the cloud. Returns a
     * reference (URI or id) identifying the uploaded snapshot.
     */
    suspend fun export(databasePath: String): Result<String>

    /**
     * Restores the database from the cloud snapshot identified by
     * [sourceUri].
     */
    suspend fun import(sourceUri: String): Result<Unit>
}
