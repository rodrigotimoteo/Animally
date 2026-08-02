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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetVaccinationDetailUseCaseTest {
    /** Mock of [IVaccinationRepository] */
    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    /** System under test [GetVaccinationDetailUseCase] */
    private lateinit var sut: GetVaccinationDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetVaccinationDetailUseCase(vaccinationRepositoryMock)
    }

    private fun newVaccination() =
        Vaccination(
            id = 5L,
            patientId = 7L,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate(2024, 4, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when vaccination exists then sut returns it`() {
        val vaccination = newVaccination()

        every { vaccinationRepositoryMock.getById(5L) } returns vaccination

        val result = sut(5L)

        assertEquals(vaccination, result)
        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.getById(5L) }
    }

    @Test
    fun `when vaccination does not exist then sut returns null`() {
        every { vaccinationRepositoryMock.getById(5L) } returns null

        val result = sut(5L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { vaccinationRepositoryMock.getById(5L) }
    }
}
