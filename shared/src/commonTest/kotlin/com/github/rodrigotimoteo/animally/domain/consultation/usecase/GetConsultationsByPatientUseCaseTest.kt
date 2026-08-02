package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
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

class GetConsultationsByPatientUseCaseTest {
    /** Mock of [IConsultationRepository] */
    private val consultationRepositoryMock: IConsultationRepository = mock()

    /** System under test [GetConsultationsByPatientUseCase] */
    private lateinit var sut: GetConsultationsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetConsultationsByPatientUseCase(consultationRepositoryMock)
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
    fun `when repository returns consultations then sut returns the same list`() {
        val consultations = listOf(newConsultation(1L), newConsultation(2L))

        every { consultationRepositoryMock.getByPatient(7L) } returns consultations

        val result = sut(7L)

        assertEquals(expected = consultations, actual = result)
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.getByPatient(7L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val consultations = emptyList<Consultation>()

        every { consultationRepositoryMock.getByPatient(7L) } returns consultations

        val result = sut(7L)

        assertEquals(expected = consultations, actual = result)
        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.getByPatient(7L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { consultationRepositoryMock.getByPatient(7L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(7L) }

        verify(VerifyMode.exactly(1)) { consultationRepositoryMock.getByPatient(7L) }
    }
}
