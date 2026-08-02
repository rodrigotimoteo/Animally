package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated vaccination.
 *
 * The next due date is always recomputed from the vaccine name and administration
 * date — also on edits. A single save path for both create and edit flows:
 * vaccinations with `id == 0L` are inserted, all others are updated.
 *
 * @param vaccinationRepository Repository instance for accessing vaccination data.
 * @param calculateNextDueDateUseCase Use case for computing the next due date.
 */
@Single
class SaveVaccinationUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
    @Provided private val calculateNextDueDateUseCase: CalculateNextDueDateUseCase,
) {
    /**
     * Persists the given [vaccination] and returns the generated identifier for new vaccinations.
     *
     * @param vaccination the vaccination to persist.
     * @return the id of the persisted vaccination.
     */
    operator fun invoke(vaccination: Vaccination): Long {
        val nextDueDate = calculateNextDueDateUseCase(vaccination.vaccineName, vaccination.dateAdministered)
        val toSave = vaccination.copy(nextDueDate = nextDueDate)
        return if (toSave.id == 0L) {
            vaccinationRepository.insert(toSave)
        } else {
            vaccinationRepository.update(toSave)
        }
    }
}
