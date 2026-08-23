package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated farrier visit.
 *
 * A single save path for both create and edit flows: visits with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param farrierVisitRepository Repository instance for accessing farrier visit data.
 */
@Single
class SaveFarrierVisitUseCase(
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [farrierVisit] and returns the generated identifier for new visits.
     *
     * @param farrierVisit the farrier visit to persist.
     * @return the id of the persisted farrier visit.
     */
    operator fun invoke(farrierVisit: FarrierVisit): Long {
        val savedId =
            if (farrierVisit.id == 0L) {
                farrierVisitRepository.insert(farrierVisit)
            } else {
                farrierVisitRepository.update(farrierVisit)
            }
        val searchableText =
            listOfNotNull(
                farrierVisit.trimOrShoe,
                farrierVisit.shoeType,
                farrierVisit.findings,
                farrierVisit.farrier,
                farrierVisit.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.FarrierVisit.wireName,
            patientId = farrierVisit.patientId,
            recordId = savedId,
            date = farrierVisit.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
