package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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

class SavePatientUseCaseTest {
    /** Mock of [IPatientRepository] */
    private val patientRepositoryMock: IPatientRepository = mock()

    /** System under test [SavePatientUseCase] */
    private lateinit var sut: SavePatientUseCase

    @BeforeTest
    fun setup() {
        sut = SavePatientUseCase(patientRepositoryMock)
    }

    private fun newPatient(id: Long) =
        Patient(
            id = id,
            name = "Midnight",
            species = "Equine",
            breed = null,
            dateOfBirth = LocalDate(2020, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { patientRepositoryMock.insertPatient(any()) } returns 42L

        val result = sut(newPatient(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.insertPatient(any()) }
        verify(VerifyMode.exactly(0)) { patientRepositoryMock.updatePatient(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { patientRepositoryMock.updatePatient(any()) } returns 1L

        val result = sut(newPatient(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { patientRepositoryMock.insertPatient(any()) }
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.updatePatient(any()) }
    }
}
