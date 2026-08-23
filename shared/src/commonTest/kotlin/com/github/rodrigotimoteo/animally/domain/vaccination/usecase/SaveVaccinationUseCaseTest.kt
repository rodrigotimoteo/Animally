package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
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

class SaveVaccinationUseCaseTest {
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private lateinit var sut: SaveVaccinationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveVaccinationUseCase(vaccinationRepositoryMock, CalculateNextDueDateUseCase(), FakeSearchRepository())
    }

    private fun newVaccination(
        id: Long = 0L,
        vaccineName: String = "Tetanus",
        nextDueDate: LocalDate? = null,
    ): Vaccination =
        Vaccination(
            id = id,
            patientId = 1L,
            vaccineName = vaccineName,
            dateAdministered = LocalDate(2024, 1, 15),
            nextDueDate = nextDueDate,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when saving new tetanus vaccination then stores computed next due date`() {
        val captured = mutableListOf<Vaccination>()
        every { vaccinationRepositoryMock.insert(any()) } calls { args ->
            captured += args.arg<Vaccination>(0)
            1L
        }

        sut(newVaccination())

        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { vaccinationRepositoryMock.update(any()) }
        assertEquals(LocalDate(2025, 1, 15), captured.single().nextDueDate)
    }

    @Test
    fun `when saving new influenza vaccination then stores 6 month due date`() {
        val captured = mutableListOf<Vaccination>()
        every { vaccinationRepositoryMock.insert(any()) } calls { args ->
            captured += args.arg<Vaccination>(0)
            1L
        }

        sut(newVaccination(vaccineName = "Influenza"))

        assertEquals(LocalDate(2024, 7, 15), captured.single().nextDueDate)
    }

    @Test
    fun `when editing vaccination then recalculates next due date`() {
        val captured = mutableListOf<Vaccination>()
        every { vaccinationRepositoryMock.update(any()) } calls { args ->
            captured += args.arg<Vaccination>(0)
            1L
        }

        sut(
            newVaccination(
                id = 7L,
                vaccineName = "Tetanus",
                nextDueDate = LocalDate(2026, 1, 1),
            ),
        )

        verify(VerifyMode.exactly(0)) { vaccinationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.update(any()) }
        assertEquals(LocalDate(2025, 1, 15), captured.single().nextDueDate)
    }
}
