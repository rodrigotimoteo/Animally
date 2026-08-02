package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
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

class GetConsultationDetailUseCaseTest {
    /** Mock of [IConsultationRepository] */
    private val consultationRepositoryMock: IConsultationRepository = mock()

    /** System under test [GetConsultationDetailUseCase] */
    private lateinit var sut: GetConsultationDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetConsultationDetailUseCase(consultationRepositoryMock)
    }

    private fun newConsultation() =
        Consultation(
            id = 5L,
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
    fun `when consultation exists then sut returns it`() {
        val consultation = newConsultation()

        every { consultationRepositoryMock.getById(5L) } returns consultation

        val result = sut(5L)

        assertEquals(consultation, result)
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.getById(5L) }
    }

    @Test
    fun `when consultation does not exist then sut returns null`() {
        every { consultationRepositoryMock.getById(5L) } returns null

        val result = sut(5L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.getById(5L) }
    }
}
