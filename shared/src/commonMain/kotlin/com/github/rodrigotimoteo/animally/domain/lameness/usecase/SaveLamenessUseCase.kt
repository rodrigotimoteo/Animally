package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated lameness evaluation.
 *
 * A single save path for both create and edit flows: lameness evaluations with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param lamenessRepository Repository instance for accessing lameness data.
 */
@Single
class SaveLamenessUseCase(
    @Provided private val lamenessRepository: ILamenessRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [lameness] and returns the generated identifier for new records.
     *
     * @param lameness the lameness evaluation to persist.
     * @return the id of the persisted lameness evaluation.
     */
    operator fun invoke(lameness: Lameness): Long {
        val savedId =
            if (lameness.id == 0L) {
                lamenessRepository.insert(lameness)
            } else {
                lamenessRepository.update(lameness)
            }
        val searchableText =
            listOfNotNull(
                lameness.gradeAAEP.toString(),
                lameness.limbLocation,
                lameness.flexionTest,
                lameness.diagnosis,
                lameness.treatment,
                lameness.vetName,
                lameness.notes,
            ).joinToString(" ")
        searchRepository.indexRecord(
            recordType = RecordType.Lameness.wireName,
            patientId = lameness.patientId,
            recordId = savedId,
            date = lameness.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
