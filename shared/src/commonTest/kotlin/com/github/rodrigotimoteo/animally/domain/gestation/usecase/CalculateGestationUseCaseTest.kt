package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateGestationUseCaseTest {
    private val sut = CalculateGestationUseCase()

    private val breedingDate = LocalDate(2024, 3, 1)

    @Test
    fun `expected due date is 340 days after breeding`() {
        val result = sut(breedingDate, breedingDate)

        assertEquals(breedingDate.plus(DatePeriod(days = 340)), result.expectedDueDate)
    }

    @Test
    fun `gestation days equals days elapsed since breeding`() {
        val today = breedingDate.plus(DatePeriod(days = 100))

        val result = sut(breedingDate, today)

        assertEquals(100, result.gestationDays)
    }

    @Test
    fun `gestation days is zero on breeding date`() {
        val result = sut(breedingDate, breedingDate)

        assertEquals(0, result.gestationDays)
    }

    @Test
    fun `gestation days is non-negative when today precedes breeding`() {
        val today = breedingDate.plus(DatePeriod(days = -10))

        val result = sut(breedingDate, today)

        assertEquals(0, result.gestationDays)
    }
}
