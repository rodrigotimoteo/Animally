package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated deworming record.
 *
 * A single save path for both create and edit flows: records with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param dewormingRepository Repository instance for accessing deworming data.
 */
@Single
class SaveDewormingUseCase(
    @Provided private val dewormingRepository: IDewormingRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [deworming] and returns the generated identifier for new records.
     *
     * @param deworming the deworming record to persist.
     * @return the id of the persisted deworming record.
     */
    operator fun invoke(deworming: Deworming): Long {
        val savedId =
            if (deworming.id == 0L) {
                dewormingRepository.insert(deworming)
            } else {
                dewormingRepository.update(deworming)
            }
        val searchableText =
            listOfNotNull(
                deworming.product,
                deworming.dose,
                deworming.vetName,
                deworming.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.Deworming.wireName,
            patientId = deworming.patientId,
            recordId = savedId,
            date = deworming.dateAdministered,
            searchableText = searchableText,
        )
        return savedId
    }
}
