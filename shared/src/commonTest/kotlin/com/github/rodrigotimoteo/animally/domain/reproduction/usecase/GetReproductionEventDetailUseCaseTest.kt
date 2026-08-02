package com.github.rodrigotimoteo.animally.domain.reproduction.usecase

import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
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

class GetReproductionEventDetailUseCaseTest {
    /** Mock of [IReproductionRepository] */
    private val reproductionRepositoryMock: IReproductionRepository = mock()

    /** System under test [GetReproductionEventDetailUseCase] */
    private lateinit var sut: GetReproductionEventDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetReproductionEventDetailUseCase(reproductionRepositoryMock)
    }

    private fun event() =
        ReproductionEvent(
            id = 7L,
            patientId = 1L,
            eventType = "Foaling",
            date = LocalDate(2024, 6, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns event then sut returns it`() {
        val event = event()

        every { reproductionRepositoryMock.getById(any()) } returns event

        val result = sut(7L)

        assertEquals(expected = event, actual = result)
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.getById(any()) }
    }

    @Test
    fun `when repository finds nothing then sut returns null`() {
        every { reproductionRepositoryMock.getById(any()) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.getById(any()) }
    }
}
