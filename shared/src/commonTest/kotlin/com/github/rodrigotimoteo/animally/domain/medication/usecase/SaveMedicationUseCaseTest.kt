package com.github.rodrigotimoteo.animally.domain.medication.usecase

import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import dev.mokkery.MockMode
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

class SaveMedicationUseCaseTest {
    private val medicationRepositoryMock: IMedicationRepository = mock()

    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private lateinit var sut: SaveMedicationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveMedicationUseCase(medicationRepositoryMock, searchRepositoryMock)
    }

    private fun newMedication(id: Long = 0L) =
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
    fun `when id is zero then sut inserts and indexes the generated id`() {
        every { medicationRepositoryMock.insert(any()) } calls { 1L }

        val result = sut(newMedication())

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { medicationRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) {
            searchRepositoryMock.indexRecord(ISearchRepository.TYPE_MEDICATION, 1L, 1L, null, "Phenylbutazone 2g")
        }
    }

    @Test
    fun `when id is non-zero then sut updates and re-indexes the medication`() {
        every { medicationRepositoryMock.update(any()) } calls { 1L }

        val result = sut(newMedication(id = 7L))

        assertEquals(7L, result)
        verify(VerifyMode.exactly(0)) { medicationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { medicationRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) {
            searchRepositoryMock.indexRecord(ISearchRepository.TYPE_MEDICATION, 1L, 7L, null, "Phenylbutazone 2g")
        }
    }
}
