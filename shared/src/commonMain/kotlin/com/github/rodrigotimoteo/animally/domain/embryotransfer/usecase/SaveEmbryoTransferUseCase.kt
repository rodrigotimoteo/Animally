package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated embryo transfer record.
 *
 * Records with `id == 0L` are inserted, all others are updated.
 */
@Single
class SaveEmbryoTransferUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
) {
    /**
     * Persists the given [record] and returns the generated identifier for new records.
     */
    operator fun invoke(record: EmbryoTransfer): Long =
        if (record.id == 0L) {
            repository.insert(record)
        } else {
            repository.update(record)
            record.id
        }
}
