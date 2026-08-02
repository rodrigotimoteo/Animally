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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetReproMedicationDetailUseCaseTest {
    /** Mock of [IReproMedicationRepository] */
    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    /** System under test [GetReproMedicationDetailUseCase] */
    private lateinit var sut: GetReproMedicationDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetReproMedicationDetailUseCase(reproMedicationRepositoryMock)
    }

    private fun reproMedication() =
        ReproMedication(
            id = 7L,
            patientId = 1L,
            medication = "Regumate",
            dateAdministered = LocalDate(2024, 6, 1),
            dosage = "1mL",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns medication then sut returns it`() {
        val reproMedication = reproMedication()

        every { reproMedicationRepositoryMock.getById(any()) } returns reproMedication

        val result = sut(7L)

        assertEquals(expected = reproMedication, actual = result)
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.getById(any()) }
    }

    @Test
    fun `when repository finds nothing then sut returns null`() {
        every { reproMedicationRepositoryMock.getById(any()) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { reproMedicationRepositoryMock.getById(any()) }
    }
}
