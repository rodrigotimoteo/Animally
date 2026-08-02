package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific vaccination.
 *
 * @param vaccinationRepository Repository instance for accessing vaccination data.
 */
@Single
class GetVaccinationDetailUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
) {
    /**
     * Retrieves detailed information for a vaccination by its ID.
     *
     * @param id The unique identifier of the vaccination to retrieve.
     * @return The [Vaccination] object if found, or `null` if no vaccination with the given ID exists.
     */
    operator fun invoke(id: Long): Vaccination? = vaccinationRepository.getById(id)
}
