package com.github.rodrigotimoteo.animally.domain.dentistry.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific dentistry record.
 *
 * @param dentistryRepository Repository instance for accessing dentistry data.
 */
@Single
class GetDentistryDetailUseCase(
    @Provided private val dentistryRepository: IDentistryRepository,
) {
    /**
     * Retrieves detailed information for a dentistry record by its ID.
     *
     * @param id The unique identifier of the dentistry record to retrieve.
     * @return The [Dentistry] object if found, or `null` if no record with the given ID exists.
     */
    operator fun invoke(id: Long): Dentistry? = dentistryRepository.getById(id)
}
