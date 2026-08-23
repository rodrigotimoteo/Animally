package com.github.rodrigotimoteo.animally.domain.farrier.usecase

import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
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

class SaveFarrierVisitUseCaseTest {
    private val farrierVisitRepositoryMock: IFarrierVisitRepository = mock()

    private lateinit var sut: SaveFarrierVisitUseCase

    @BeforeTest
    fun setup() {
        sut = SaveFarrierVisitUseCase(farrierVisitRepositoryMock, FakeSearchRepository())
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
    fun `when id is zero then sut inserts and returns generated id`() {
        every { farrierVisitRepositoryMock.insert(any()) } returns 42L

        val result = sut(newFarrierVisit(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { farrierVisitRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { farrierVisitRepositoryMock.update(any()) } returns 1L

        val result = sut(newFarrierVisit(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { farrierVisitRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { farrierVisitRepositoryMock.update(any()) }
    }
}
