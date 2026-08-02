package com.github.rodrigotimoteo.animally.domain.vaccination.usecase

import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetVaccinationsByPatientUseCaseTest {
    /** Mock of [IVaccinationRepository] */
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    /** System under test [GetVaccinationsByPatientUseCase] */
    private lateinit var sut: GetVaccinationsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetVaccinationsByPatientUseCase(vaccinationRepositoryMock)
    }

    private fun newVaccination(id: Long) =
        Vaccination(
            id = id,
            patientId = 7L,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate(2024, 4, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns vaccinations then sut returns the same list`() {
        val vaccinations = listOf(newVaccination(1L), newVaccination(2L))

        every { vaccinationRepositoryMock.getByPatient(7L) } returns vaccinations

        val result = sut(7L)

        assertEquals(expected = vaccinations, actual = result)
        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.getByPatient(7L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val vaccinations = emptyList<Vaccination>()

        every { vaccinationRepositoryMock.getByPatient(7L) } returns vaccinations

        val result = sut(7L)

        assertEquals(expected = vaccinations, actual = result)
        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.getByPatient(7L) }
    }
}
