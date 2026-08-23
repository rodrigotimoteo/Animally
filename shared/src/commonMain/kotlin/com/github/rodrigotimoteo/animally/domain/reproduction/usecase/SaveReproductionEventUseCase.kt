package com.github.rodrigotimoteo.animally.domain.reproduction.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated reproduction event.
 *
 * A single save path for both create and edit flows: reproduction events with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param reproductionRepository Repository instance for accessing reproduction event data.
 */
@Single
class SaveReproductionEventUseCase(
    @Provided private val reproductionRepository: IReproductionRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [reproductionEvent] and returns the generated identifier for new events.
     *
     * @param reproductionEvent the reproduction event to persist.
     * @return the id of the persisted reproduction event.
     */
    operator fun invoke(reproductionEvent: ReproductionEvent): Long {
        val savedId =
            if (reproductionEvent.id == 0L) {
                reproductionRepository.insert(reproductionEvent)
            } else {
                reproductionRepository.update(reproductionEvent)
            }
        val searchableText =
            listOfNotNull(
                reproductionEvent.eventType,
                reproductionEvent.details,
                reproductionEvent.initialExamFindings,
                reproductionEvent.stallionName,
                reproductionEvent.breedingType,
                reproductionEvent.vetName,
                reproductionEvent.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.ReproductionEvent.wireName,
            patientId = reproductionEvent.patientId,
            recordId = savedId,
            date = reproductionEvent.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
