package com.github.rodrigotimoteo.animally.domain.sync

import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Application-facing entry point for a manual or scheduled sync.
 */
@Single
class SyncUseCase(
    @Provided private val engine: SyncEngine,
) {
    suspend operator fun invoke(): SyncResult = engine.sync()
}
