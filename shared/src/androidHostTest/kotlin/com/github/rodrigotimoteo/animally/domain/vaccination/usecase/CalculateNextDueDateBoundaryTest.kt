package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Host-JVM boundary tests for [CalculateNextDueDateUseCase] month arithmetic:
 * day-of-month clamping when the target month has fewer days.
 *
 * Interval selection itself is covered by commonTest's CalculateNextDueDateUseCaseTest.
 */
class CalculateNextDueDateBoundaryTest {
    private val sut = CalculateNextDueDateUseCase()

    @Test
    fun givenMonthEndDay31WhenSixMonthIntervalThenLandsOnDay31OfTargetMonth() {
        // July has 31 days, so no clamping is needed.
        assertEquals(LocalDate(2024, 7, 31), sut("Influenza", LocalDate(2024, 1, 31)))
    }

    @Test
    fun givenAug31WhenSixMonthIntervalThenClampsToLeapDayFeb29() {
        // Aug 31 + 6 months = Feb 31, which does not exist in leap year 2024 -> clamps to Feb 29.
        assertEquals(LocalDate(2024, 2, 29), sut("Influenza", LocalDate(2023, 8, 31)))
    }

    @Test
    fun givenAug31WhenSixMonthIntervalThenClampsToFeb28NonLeapTargetYear() {
        // Aug 31 + 6 months = Feb 31, which does not exist in 2025 -> clamps to Feb 28.
        assertEquals(LocalDate(2025, 2, 28), sut("Influenza", LocalDate(2024, 8, 31)))
    }

    @Test
    fun givenLeapDayWhenTwelveMonthIntervalThenClampsToFeb28NextYear() {
        assertEquals(LocalDate(2025, 2, 28), sut("Tetanus", LocalDate(2024, 2, 29)))
    }

    @Test
    fun givenDec31WhenAnnualIntervalThenLandsOnDec31NextYear() {
        assertEquals(LocalDate(2025, 12, 31), sut("Tetanus", LocalDate(2024, 12, 31)))
    }
}
