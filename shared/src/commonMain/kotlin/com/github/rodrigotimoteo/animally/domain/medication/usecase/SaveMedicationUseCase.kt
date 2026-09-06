package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated medication.
 *
 * A single save path for both create and edit flows: medications with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param medicationRepository Repository instance for accessing medication data.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class SaveMedicationUseCase(
    @Provided private val medicationRepository: IMedicationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [medication] and returns the generated identifier for new records.
     *
     * @param medication the medication to persist.
     * @return the id of the persisted medication.
     */
    operator fun invoke(medication: Medication): Long {
        val savedId =
            if (medication.id == 0L) {
                medicationRepository.insert(medication)
            } else {
                medicationRepository.update(medication)
                medication.id
            }
        searchRepository.indexRecord(
            recordType = ISearchRepository.TYPE_MEDICATION,
            patientId = medication.patientId,
            recordId = savedId,
            date = medication.startDate,
            searchableText = SearchableText.medication(medication),
        )
        return savedId
    }
}
