package com.github.rodrigotimoteo.animally.domain.reproduction.usecase

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
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

class GetReproductionEventsByPatientUseCaseTest {
    /** Mock of [IReproductionRepository] */
    private val reproductionRepositoryMock: IReproductionRepository = mock()

    /** System under test [GetReproductionEventsByPatientUseCase] */
    private lateinit var sut: GetReproductionEventsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetReproductionEventsByPatientUseCase(reproductionRepositoryMock)
    }

    private fun event(
        id: Long,
        date: LocalDate,
    ) = ReproductionEvent(
        id = id,
        patientId = 1L,
        eventType = "Breeding",
        date = date,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `when repository returns events then sut returns the same list`() {
        val events =
            listOf(
                event(id = 1L, date = LocalDate(2024, 3, 1)),
                event(id = 2L, date = LocalDate(2024, 2, 1)),
            )

        every { reproductionRepositoryMock.getByPatient(any()) } returns events

        val result = sut(1L)

        assertEquals(expected = events, actual = result)
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val events = emptyList<ReproductionEvent>()

        every { reproductionRepositoryMock.getByPatient(any()) } returns events

        val result = sut(1L)

        assertEquals(expected = events, actual = result)
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { reproductionRepositoryMock.getByPatient(any()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.getByPatient(any()) }
    }
}
