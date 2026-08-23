package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated reproduction medication.
 *
 * A single save path for both create and edit flows: reproduction medications with
 * `id == 0L` are inserted, all others are updated.
 *
 * @param reproMedicationRepository Repository instance for accessing reproduction medication data.
 */
@Single
class SaveReproMedicationUseCase(
    @Provided private val reproMedicationRepository: IReproMedicationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [reproMedication] and returns the generated identifier for new records.
     *
     * @param reproMedication the reproduction medication to persist.
     * @return the id of the persisted reproduction medication.
     */
    operator fun invoke(reproMedication: ReproMedication): Long {
        val savedId =
            if (reproMedication.id == 0L) {
                reproMedicationRepository.insert(reproMedication)
            } else {
                reproMedicationRepository.update(reproMedication)
            }
        val searchableText =
            listOfNotNull(
                reproMedication.medication,
                reproMedication.dosage,
                reproMedication.purpose,
                reproMedication.vetName,
                reproMedication.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.ReproMedication.wireName,
            patientId = reproMedication.patientId,
            recordId = savedId,
            date = reproMedication.dateAdministered,
            searchableText = searchableText,
        )
        return savedId
    }
}
