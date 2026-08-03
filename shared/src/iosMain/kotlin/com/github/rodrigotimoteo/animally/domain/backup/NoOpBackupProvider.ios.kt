package com.github.rodrigotimoteo.animally.domain.backup

import org.koin.core.annotation.Single

/**
 * iOS no-op implementation of [BackupProvider] for the ROD-42 POC.
 * Real iCloud backup is deferred beyond the POC.
 */
@Single(binds = [BackupProvider::class])
class NoOpBackupProvider : BackupProvider {
    override suspend fun export(databasePath: String): Result<String> = Result.success("not-implemented")

    override suspend fun import(sourceUri: String): Result<Unit> = Result.success(Unit)
}
