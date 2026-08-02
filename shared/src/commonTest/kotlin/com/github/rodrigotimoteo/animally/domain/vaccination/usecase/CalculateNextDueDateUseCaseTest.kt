package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateNextDueDateUseCaseTest {
    private val sut = CalculateNextDueDateUseCase()

    private val administeredOn = LocalDate(2024, 1, 15)

    @Test
    fun `tetanus gets 12 month interval`() {
        val result = sut("Tetanus", administeredOn)

        assertEquals(LocalDate(2025, 1, 15), result)
    }

    @Test
    fun `influenza gets 6 month interval`() {
        val result = sut("Influenza", administeredOn)

        assertEquals(LocalDate(2024, 7, 15), result)
    }

    @Test
    fun `rhinopneumonitis gets 6 month interval`() {
        val result = sut("Rhinopneumonitis", administeredOn)

        assertEquals(LocalDate(2024, 7, 15), result)
    }

    @Test
    fun `unknown vaccine defaults to 12 month interval`() {
        val result = sut("Unknown Vaccine", administeredOn)

        assertEquals(LocalDate(2025, 1, 15), result)
    }

    @Test
    fun `matching is case-insensitive and partial`() {
        assertEquals(LocalDate(2025, 1, 15), sut("tetanus toxoid", administeredOn))
        assertEquals(LocalDate(2024, 7, 15), sut("equine influenza", administeredOn))
    }
}
