package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import dev.mokkery.MockMode
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

class SaveConsultationUseCaseTest {
    /** Mock of [IConsultationRepository] */
    private val consultationRepositoryMock: IConsultationRepository = mock()

    /** Mock of [ISearchRepository] */
    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    /** System under test [SaveConsultationUseCase] */
    private lateinit var sut: SaveConsultationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveConsultationUseCase(consultationRepositoryMock, searchRepositoryMock)
    }

    private fun newConsultation(id: Long) =
        Consultation(
            id = id,
            patientId = 7L,
            date = LocalDate(2024, 4, 1),
            subjective = "Subjective",
            objective = "Objective",
            assessment = "Assessment",
            plan = "Plan",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and indexes the generated id`() {
        every { consultationRepositoryMock.insert(any()) } returns 42L

        val result = sut(newConsultation(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { consultationRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) {
            searchRepositoryMock.indexRecord(
                ISearchRepository.TYPE_CONSULTATION,
                7L,
                42L,
                LocalDate(2024, 4, 1),
                "Assessment Plan",
            )
        }
    }

    @Test
    fun `when id is non-zero then sut updates and re-indexes the consultation`() {
        every { consultationRepositoryMock.update(any()) } returns 1L

        val result = sut(newConsultation(id = 5L))

        assertEquals(5L, result)
        verify(VerifyMode.exactly(0)) { consultationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.update(any()) }
        verify(VerifyMode.exactly(1)) {
            searchRepositoryMock.indexRecord(
                ISearchRepository.TYPE_CONSULTATION,
                7L,
                5L,
                LocalDate(2024, 4, 1),
                "Assessment Plan",
            )
        }
    }
}
