package com.github.rodrigotimoteo.animally.domain.dentistry.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated dentistry record.
 *
 * A single save path for both create and edit flows: records with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param dentistryRepository Repository instance for accessing dentistry data.
 */
@Single
class SaveDentistryUseCase(
    @Provided private val dentistryRepository: IDentistryRepository,
) {
    /**
     * Persists the given [dentistry] and returns the generated identifier for new records.
     *
     * @param dentistry the dentistry record to persist.
     * @return the id of the persisted dentistry record.
     */
    operator fun invoke(dentistry: Dentistry): Long =
        if (dentistry.id == 0L) {
            dentistryRepository.insert(dentistry)
        } else {
            dentistryRepository.update(dentistry)
        }
}
