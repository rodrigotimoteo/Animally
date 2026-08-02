package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated controlled substance record.
 *
 * A single save path for both create and edit flows: records with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param substanceRepository Repository instance for accessing controlled substance data.
 */
@Single
class SaveControlledSubstanceUseCase(
    @Provided private val substanceRepository: IControlledSubstanceRepository,
) {
    /**
     * Persists the given [controlledSubstance] and returns the generated identifier for new records.
     *
     * @param controlledSubstance the controlled substance record to persist.
     * @return the id of the persisted controlled substance record.
     */
    operator fun invoke(controlledSubstance: ControlledSubstance): Long =
        if (controlledSubstance.id == 0L) {
            substanceRepository.insert(controlledSubstance)
        } else {
            substanceRepository.update(controlledSubstance)
        }
}
