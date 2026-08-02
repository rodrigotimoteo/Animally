package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.core.annotation.Single

/**
 * Use case for computing the next vaccination due date.
 *
 * The interval depends on the vaccine type. Matching is case-insensitive on the
 * vaccine name; unknown vaccines default to a 12-month interval.
 *
 * Intervals:
 * - Tetanus: 12 months.
 * - Influenza: 6 months.
 * - Rhinopneumonitis: 6 months.
 * - Any other vaccine: 12 months.
 */
@Single
class CalculateNextDueDateUseCase {
    private companion object {
        const val SIX_MONTH_INTERVAL = 6
        const val ANNUAL_INTERVAL = 12
    }

    /**
     * Computes the next due date for the given [vaccineName] administered on [dateAdministered].
     *
     * @param vaccineName the name of the administered vaccine.
     * @param dateAdministered the date the vaccine was administered.
     * @return the computed next due date.
     */
    operator fun invoke(
        vaccineName: String,
        dateAdministered: LocalDate,
    ): LocalDate {
        val months =
            when {
                vaccineName.contains("Tetanus", ignoreCase = true) -> ANNUAL_INTERVAL
                vaccineName.contains("Influenza", ignoreCase = true) -> SIX_MONTH_INTERVAL
                vaccineName.contains("Rhinopneumonitis", ignoreCase = true) -> SIX_MONTH_INTERVAL
                else -> ANNUAL_INTERVAL
            }
        return dateAdministered.plus(DatePeriod(months = months))
    }
}
