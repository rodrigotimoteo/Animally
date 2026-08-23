package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated surgery.
 *
 * A single save path for both create and edit flows: surgeries with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param surgeryRepository Repository instance for accessing surgery data.
 */
@Single
class SaveSurgeryUseCase(
    @Provided private val surgeryRepository: ISurgeryRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [surgery] and returns the generated identifier for new records.
     *
     * @param surgery the surgery to persist.
     * @return the id of the persisted surgery.
     */
    operator fun invoke(surgery: Surgery): Long {
        val savedId =
            if (surgery.id == 0L) {
                surgeryRepository.insert(surgery)
            } else {
                surgeryRepository.update(surgery)
            }
        val searchableText =
            listOfNotNull(
                surgery.type,
                surgery.description,
                surgery.outcome,
                surgery.surgeon,
                surgery.anesthesia,
                surgery.analgesia,
                surgery.complications,
                surgery.recoveryNotes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.Surgery.wireName,
            patientId = surgery.patientId,
            recordId = savedId,
            date = surgery.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
