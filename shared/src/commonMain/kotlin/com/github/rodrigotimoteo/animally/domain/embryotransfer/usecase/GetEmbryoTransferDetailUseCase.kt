package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving a single embryo transfer record.
 */
@Single
class GetEmbryoTransferDetailUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
) {
    /**
     * Returns the active record with [id], or `null` when not found.
     */
    operator fun invoke(id: Long): EmbryoTransfer? = repository.getById(id)
}
