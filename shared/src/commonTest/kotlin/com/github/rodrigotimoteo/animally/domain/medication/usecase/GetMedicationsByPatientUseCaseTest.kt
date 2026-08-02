package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
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

class GetMedicationsByPatientUseCaseTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()

    private lateinit var sut: GetMedicationsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetMedicationsByPatientUseCase(medicationRepositoryMock)
    }

    private fun newMedication(id: Long) =
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
    fun `when repository returns list then sut returns the same list`() {
        val medications = listOf(newMedication(1L), newMedication(2L))

        every { medicationRepositoryMock.getByPatient(1L) } returns medications

        val result = sut(1L)

        assertEquals(medications, result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { medicationRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList<Medication>(), result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { medicationRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }
    }
}
