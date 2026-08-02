package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveMedicationUseCaseTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()

    private lateinit var sut: SaveMedicationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveMedicationUseCase(medicationRepositoryMock)
    }

    private fun newMedication(id: Long = 0L) =
        Medication(
            id = id,
            patientId = 1L,
            name = "Phenylbutazone",
            dosage = "2g",
            startDate = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts`() {
        every { medicationRepositoryMock.insert(any()) } calls { 1L }

        val result = sut(newMedication())

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { medicationRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates`() {
        every { medicationRepositoryMock.update(any()) } calls { 1L }

        val result = sut(newMedication(id = 7L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.update(any()) }
    }
}
