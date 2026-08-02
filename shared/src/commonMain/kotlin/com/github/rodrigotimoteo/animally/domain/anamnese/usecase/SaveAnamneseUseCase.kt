package com.github.rodrigotimoteo.animally.domain.anamnese.usecase

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a patient's anamnese record.
 *
 * The save is an upsert: saving an anamnese for a patient that already has one
 * updates the existing row.
 *
 * @param anamneseRepository Repository instance for accessing anamnese data.
 */
@Single
class SaveAnamneseUseCase(
    @Provided private val anamneseRepository: IAnamneseRepository,
) {
    /**
     * Persists the given [anamnese] and returns the generated identifier for new records.
     *
     * @param anamnese the anamnese to persist.
     * @return the id of the persisted anamnese.
     */
    operator fun invoke(anamnese: Anamnese): Long = anamneseRepository.save(anamnese)
}
