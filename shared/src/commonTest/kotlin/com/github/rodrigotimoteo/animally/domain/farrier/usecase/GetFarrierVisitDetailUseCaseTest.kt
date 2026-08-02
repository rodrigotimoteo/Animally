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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetFarrierVisitDetailUseCaseTest {
    private val farrierVisitRepositoryMock: IFarrierVisitRepository = mock()

    private lateinit var sut: GetFarrierVisitDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetFarrierVisitDetailUseCase(farrierVisitRepositoryMock)
    }

    private fun newFarrierVisit() =
        FarrierVisit(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2024, 3, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when farrier visit exists then sut returns it`() {
        val visit = newFarrierVisit()

        every { farrierVisitRepositoryMock.getById(7L) } returns visit

        val result = sut(7L)

        assertEquals(visit, result)
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.getById(7L) }
    }

    @Test
    fun `when farrier visit does not exist then sut returns null`() {
        every { farrierVisitRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.getById(7L) }
    }
}
