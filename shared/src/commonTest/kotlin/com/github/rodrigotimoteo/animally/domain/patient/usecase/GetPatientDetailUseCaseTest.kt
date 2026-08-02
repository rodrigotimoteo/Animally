package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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

class GetPatientDetailUseCaseTest {
    /** Mock of [IPatientRepository] */
    private val patientRepositoryMock: IPatientRepository = mock()

    /** System under test [GetPatientDetailUseCase] */
    private lateinit var sut: GetPatientDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetPatientDetailUseCase(patientRepositoryMock)
    }

    private fun newPatient() =
        Patient(
            id = 3L,
            name = "Midnight",
            species = "Equine",
            breed = null,
            dateOfBirth = LocalDate(2020, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when patient exists then sut returns it`() {
        val patient = newPatient()

        every { patientRepositoryMock.getPatientById(3L) } returns patient

        val result = sut(3L)

        assertEquals(patient, result)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.getPatientById(3L) }
    }

    @Test
    fun `when patient does not exist then sut returns null`() {
        every { patientRepositoryMock.getPatientById(3L) } returns null

        val result = sut(3L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.getPatientById(3L) }
    }
}
