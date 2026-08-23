package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param gestationRepository Repository instance for accessing the records.
 */
@Single
class DeleteGestationUseCase(
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        gestationRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.Gestation.wireName, id)
    }
}
