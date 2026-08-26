package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting an embryo transfer record by marking it inactive.
 */
@Single
class DeleteEmbryoTransferUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     */
    operator fun invoke(id: Long) {
        val rows = repository.setInactive(id, Clock.System.now())
        if (rows > 0L) {
            searchRepository.deleteRecord(RecordType.EmbryoTransfer.wireName, id)
        }
    }
}
