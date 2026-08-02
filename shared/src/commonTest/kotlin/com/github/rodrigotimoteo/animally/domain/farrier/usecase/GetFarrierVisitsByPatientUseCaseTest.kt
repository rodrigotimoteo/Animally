package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetFarrierVisitsByPatientUseCaseTest {
    private val farrierVisitRepositoryMock: IFarrierVisitRepository = mock()

    private lateinit var sut: GetFarrierVisitsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetFarrierVisitsByPatientUseCase(farrierVisitRepositoryMock)
    }

    private fun newFarrierVisit(id: Long) =
        FarrierVisit(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 3, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns farrier visits then sut returns them`() {
        val visits = listOf(newFarrierVisit(1L), newFarrierVisit(2L))

        every { farrierVisitRepositoryMock.getByPatient(1L) } returns visits

        val result = sut(1L)

        assertEquals(visits, result)
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { farrierVisitRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList(), result)
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.getByPatient(1L) }
    }
}
