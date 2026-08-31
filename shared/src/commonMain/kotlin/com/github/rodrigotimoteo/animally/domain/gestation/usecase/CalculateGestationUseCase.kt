package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.model.GestationStatus
import com.github.rodrigotimoteo.animally.domain.gestation.model.isActiveGestation
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
        val expectedDueDate = breedingDate.plus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS))
        val gestationDays = breedingDate.daysUntil(today).coerceAtLeast(0)
        return GestationProgress(
            expectedDueDate = expectedDueDate,
            gestationDays = gestationDays,
        )
    }

    /**
     * Applies the current derived progress to an active gestation for display.
     *
     * Overwrites both [Gestation.expectedDueDate] (breedingDate + 340) and
     * [Gestation.gestationDays] with deterministic recalculation from [today].
     * Persisted expectedDueDate may be stale if breedingDate was edited or earlier
     * calc used wrong period; recalc ensures stored and derived agree for display.
     * Completed, failed, and foaled records retain their recorded day count so
     * historical pregnancies do not appear to keep progressing after they
     * have ended. This is an in-memory projection; it does not rewrite the
     * persisted record or create sync churn.
     */
    fun withCurrentProgress(
        gestation: Gestation,
        today: LocalDate,
    ): Gestation {
        if (!gestation.isActiveGestation()) return gestation

        val progress = invoke(gestation.breedingDate, today)
        return gestation.copy(
            expectedDueDate = progress.expectedDueDate,
            gestationDays = progress.gestationDays,
        )
    }
}
