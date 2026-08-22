package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a record by marking it inactive.
 *
 * @param vaccinationRepository Repository instance for accessing the records.
 */
@Single
class DeleteVaccinationUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
) {
    /**
     * Marks the record identified by [id] as inactive.
     *
     * @param id the identifier of the record to delete.
     */
    operator fun invoke(id: Long) {
        vaccinationRepository.setInactive(id, Clock.System.now())
    }
}
