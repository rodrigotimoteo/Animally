package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import dev.mokkery.answering.returns
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

class SaveReproMedicationUseCaseTest {
    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    private lateinit var sut: SaveReproMedicationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveReproMedicationUseCase(reproMedicationRepositoryMock)
    }

    private fun newReproMedication(id: Long) =
        ReproMedication(
            id = id,
            patientId = 1L,
            medication = "OxyContin",
            dateAdministered = LocalDate(2024, 3, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { reproMedicationRepositoryMock.insert(any()) } returns 42L

        val result = sut(newReproMedication(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { reproMedicationRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { reproMedicationRepositoryMock.update(any()) } returns 1L

        val result = sut(newReproMedication(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { reproMedicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.update(any()) }
    }
}
