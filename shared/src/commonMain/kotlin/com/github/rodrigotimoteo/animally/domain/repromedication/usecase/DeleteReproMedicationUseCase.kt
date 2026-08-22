package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param reproMedicationRepository Repository instance for accessing the records.
 */
@Single
class DeleteReproMedicationUseCase(
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        reproMedicationRepository.setInactive(id, Clock.System.now())
    }
}
