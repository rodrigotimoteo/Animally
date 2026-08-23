package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated gestation record.
 *
 * Recomputes the expected due date and gestation day count from the breeding
 * date before saving. Gestations with `id == 0L` are inserted, all others are
 * updated.
 *
 * @param gestationRepository Repository instance for accessing gestation data.
 * @param calculateGestationUseCase Use case for computing gestation progress.
 */
@Single
class SaveGestationUseCase(
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val calculateGestationUseCase: CalculateGestationUseCase,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [gestation] and returns the generated identifier for new records.
     *
     * @param gestation the gestation record to persist.
     * @param today the reference date for the gestation day count.
     * @return the id of the persisted gestation record.
     */
    operator fun invoke(
        gestation: Gestation,
        today: LocalDate,
    ): Long {
        val progress = calculateGestationUseCase(gestation.breedingDate, today)
        val updatedGestation =
            gestation.copy(
                expectedDueDate = progress.expectedDueDate,
                gestationDays = progress.gestationDays,
            )
        val savedId =
            if (updatedGestation.id == 0L) {
                gestationRepository.insert(updatedGestation)
            } else {
                gestationRepository.update(updatedGestation)
            }
        searchRepository.indexRecord(
            recordType = RecordType.Gestation.wireName,
            patientId = updatedGestation.patientId,
            recordId = savedId,
            date = updatedGestation.breedingDate,
            searchableText = listOfNotNull(updatedGestation.status, updatedGestation.notes).joinToString(" "),
        )
        return savedId
    }
}
