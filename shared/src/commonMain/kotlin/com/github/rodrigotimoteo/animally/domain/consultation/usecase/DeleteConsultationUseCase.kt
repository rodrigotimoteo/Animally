package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param consultationRepository Repository instance for accessing the records.
 * @param searchRepository Removes the record from the global search index.
 */
@Single
class DeleteConsultationUseCase(
    @Provided private val consultationRepository: IConsultationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the record identified by [id] as inactive and drops it from the
     * search index so deleted records stop matching queries.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        consultationRepository.setInactive(id, Clock.System.now())
        searchRepository.deleteRecord(ISearchRepository.TYPE_CONSULTATION, id)
    }
}
