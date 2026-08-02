package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetPatientListUseCaseTest {
    /** Mock of [IPatientRepository] */
    private val patientRepositoryMock: IPatientRepository = mock()

    /** System under test [GetPatientListUseCase] */
    private lateinit var sut: GetPatientListUseCase

    @BeforeTest
    fun setup() {
        sut = GetPatientListUseCase(patientRepositoryMock)
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
    fun `when repository returns patients then sut returns the same list`() {
        val patients = listOf(newPatient(1L), newPatient(2L))

        every { patientRepositoryMock.getPatientList() } returns patients

        val result = sut()

        assertEquals(expected = patients, actual = result)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.getPatientList() }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val patients = emptyList<Patient>()

        every { patientRepositoryMock.getPatientList() } returns patients

        val result = sut()

        assertEquals(expected = patients, actual = result)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.getPatientList() }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { patientRepositoryMock.getPatientList() } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut() }

        verify(VerifyMode.exactly(1)) { patientRepositoryMock.getPatientList() }
    }
}
