package com.github.rodrigotimoteo.animally.domain.weight.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated weight entry.
 *
 * A single save path for both create and edit flows: entries with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param weightRepository Repository instance for accessing weight data.
 */
@Single
class SaveWeightUseCase(
    @Provided private val weightRepository: IWeightRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [weight] and returns the generated identifier for new entries.
     *
     * @param weight the weight entry to persist.
     * @return the id of the persisted weight entry.
     */
    operator fun invoke(weight: Weight): Long {
        val savedId =
            if (weight.id == 0L) {
                weightRepository.insert(weight)
            } else {
                weightRepository.update(weight)
            }
        searchRepository.indexRecord(
            recordType = RecordType.Weight.wireName,
            patientId = weight.patientId,
            recordId = savedId,
            date = weight.date,
            searchableText = listOfNotNull(weight.weightKg.toString(), weight.notes).joinToString(" "),
        )
        return savedId
    }
}
