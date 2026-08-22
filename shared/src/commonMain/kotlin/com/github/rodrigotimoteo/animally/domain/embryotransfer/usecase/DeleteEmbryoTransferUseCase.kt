package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting an embryo transfer record by marking it inactive.
 */
@Single
class DeleteEmbryoTransferUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     */
    operator fun invoke(id: Long) {
        repository.setInactive(id, Clock.System.now())
    }
}
