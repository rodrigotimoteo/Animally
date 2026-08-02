package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class GetMedicationDetailUseCaseTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()

    private lateinit var sut: GetMedicationDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetMedicationDetailUseCase(medicationRepositoryMock)
    }

    private fun newMedication() =
        Medication(
            id = 7L,
            patientId = 1L,
            name = "Phenylbutazone",
            dosage = "2g",
            startDate = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when medication exists then sut returns it`() {
        val medication = newMedication()

        every { medicationRepositoryMock.getById(7L) } returns medication

        val result = sut(7L)

        assertEquals(medication, result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.getById(7L) }
    }

    @Test
    fun `when medication does not exist then sut returns null`() {
        every { medicationRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.getById(7L) }
    }
}
