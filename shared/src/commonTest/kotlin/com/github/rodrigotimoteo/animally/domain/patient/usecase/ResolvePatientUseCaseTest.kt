package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class ResolvePatientUseCaseTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private lateinit var sut: ResolvePatientUseCase

    @BeforeTest
    fun setup() {
        sut = ResolvePatientUseCase(patientRepositoryMock)
    }

    private fun patient(
        id: Long,
        name: String,
    ) = Patient(
        id = id,
        name = name,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    @Test
    fun `when exact match then resolved`() {
        every { patientRepositoryMock.getPatientList() } returns listOf(patient(1L, "Trovão"))

        val result = sut("Trovão")

        assertEquals(PatientResolution.Resolved::class, result::class)
        assertEquals(1L, (result as PatientResolution.Resolved).patient.id)
    }

    @Test
    fun `when case differs then resolved`() {
        every { patientRepositoryMock.getPatientList() } returns listOf(patient(1L, "Trovao"))

        val result = sut("trovao")

        assertEquals(1L, (result as PatientResolution.Resolved).patient.id)
    }

    @Test
    fun `when diacritics differ then resolved`() {
        every { patientRepositoryMock.getPatientList() } returns listOf(patient(1L, "Trovão"))

        val result = sut("Trovao")

        assertEquals(1L, (result as PatientResolution.Resolved).patient.id)
    }

    @Test
    fun `when several patients share normalized name then ambiguous with candidates`() {
        every {
            patientRepositoryMock.getPatientList()
        } returns listOf(patient(1L, "Trovão"), patient(2L, "TROVAO"))

        val result = sut("Trovao")

        val ambiguous = assertIs<PatientResolution.Ambiguous>(result)
        assertEquals(listOf(1L, 2L), ambiguous.candidates.map { it.id })
    }

    @Test
    fun `when no match then not found`() {
        every { patientRepositoryMock.getPatientList() } returns listOf(patient(1L, "Relâmpago"))

        val result = sut("Trovao")

        assertEquals(PatientResolution.NotFound, result)
    }
}
