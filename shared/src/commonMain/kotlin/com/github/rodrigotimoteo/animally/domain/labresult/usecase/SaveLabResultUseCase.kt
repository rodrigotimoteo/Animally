package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated lab result.
 *
 * A single save path for both create and edit flows: lab results with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param labResultRepository Repository instance for accessing lab result data.
 */
@Single
class SaveLabResultUseCase(
    @Provided private val labResultRepository: ILabResultRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [labResult] and returns the generated identifier for new lab results.
     *
     * @param labResult the lab result to persist.
     * @return the id of the persisted lab result.
     */
    operator fun invoke(labResult: LabResult): Long {
        val savedId =
            if (labResult.id == 0L) {
                labResultRepository.insert(labResult)
            } else {
                labResultRepository.update(labResult)
            }
        val searchableText =
            listOfNotNull(
                labResult.testType,
                labResult.results,
                labResult.normalRange,
                labResult.vetName,
                labResult.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.LabResult.wireName,
            patientId = labResult.patientId,
            recordId = savedId,
            date = labResult.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
