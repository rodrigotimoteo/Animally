package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.koin.core.annotation.Single

/**
 * Computed progress values for a gestation record.
 *
 * @property expectedDueDate the expected foaling date.
 * @property gestationDays the number of days elapsed since breeding.
 */
data class GestationProgress(
    val expectedDueDate: LocalDate,
    val gestationDays: Int,
)

/**
 * Use case for computing gestation progress from a breeding date.
 *
 * The expected due date is fixed at 340 days after breeding, matching the
 * approximate equine gestation period.
 */
@Single
class CalculateGestationUseCase {
    private companion object {
        const val GESTATION_PERIOD_DAYS = 340
    }

    /**
     * Computes the gestation progress for the given [breedingDate] as of [today].
     *
     * @param breedingDate the date the mare was bred.
     * @param today the reference date for the day count.
     * @return the computed [GestationProgress].
     */
    operator fun invoke(
        breedingDate: LocalDate,
        today: LocalDate,
    ): GestationProgress {
        val expectedDueDate = breedingDate.plus(DatePeriod(days = GESTATION_PERIOD_DAYS))
        val gestationDays = breedingDate.daysUntil(today).coerceAtLeast(0)
        return GestationProgress(
            expectedDueDate = expectedDueDate,
            gestationDays = gestationDays,
        )
    }
}
