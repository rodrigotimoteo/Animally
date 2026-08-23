package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param medicationRepository Repository instance for accessing the records.
 * @param searchRepository Removes the record from the global search index.
 */
@Single
class DeleteMedicationUseCase(
    @Provided private val medicationRepository: IMedicationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive and drops it from the
     * search index so deleted records stop matching queries.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        medicationRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(ISearchRepository.TYPE_MEDICATION, id)
    }
}
