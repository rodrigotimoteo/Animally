package com.github.rodrigotimoteo.animally.domain.repromedication.usecase

import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetReproMedicationsByPatientUseCaseTest {
    /** Mock of [IReproMedicationRepository] */
    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    /** System under test [GetReproMedicationsByPatientUseCase] */
    private lateinit var sut: GetReproMedicationsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetReproMedicationsByPatientUseCase(reproMedicationRepositoryMock)
    }

    private fun reproMedication(
        id: Long,
        dateAdministered: LocalDate,
    ) = ReproMedication(
        id = id,
        patientId = 1L,
        medication = "OxyContin",
        dateAdministered = dateAdministered,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `when repository returns medications then sut returns the same list`() {
        val medications =
            listOf(
                reproMedication(id = 1L, dateAdministered = LocalDate(2024, 3, 1)),
                reproMedication(id = 2L, dateAdministered = LocalDate(2024, 2, 1)),
            )

        every { reproMedicationRepositoryMock.getByPatient(any()) } returns medications

        val result = sut(1L)

        assertEquals(expected = medications, actual = result)
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val medications = emptyList<ReproMedication>()

        every { reproMedicationRepositoryMock.getByPatient(any()) } returns medications

        val result = sut(1L)

        assertEquals(expected = medications, actual = result)
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { reproMedicationRepositoryMock.getByPatient(any()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.getByPatient(any()) }
    }
}
