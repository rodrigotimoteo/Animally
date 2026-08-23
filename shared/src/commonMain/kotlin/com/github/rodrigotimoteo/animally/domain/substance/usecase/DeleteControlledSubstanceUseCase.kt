package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param substanceRepository Repository instance for accessing the records.
 */
@Single
class DeleteControlledSubstanceUseCase(
    @Provided private val substanceRepository: IControlledSubstanceRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        substanceRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(RecordType.ControlledSubstance.wireName, id)
    }
}
