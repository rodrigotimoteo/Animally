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
import kotlin.time.Instant

class SaveReproductionEventUseCaseTest {
    private val reproductionRepositoryMock: IReproductionRepository = mock()

    private lateinit var sut: SaveReproductionEventUseCase

    @BeforeTest
    fun setup() {
        sut = SaveReproductionEventUseCase(reproductionRepositoryMock)
    }

    private fun newEvent(id: Long) =
        ReproductionEvent(
            id = id,
            patientId = 1L,
            eventType = "Heat",
            date = LocalDate(2024, 3, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { reproductionRepositoryMock.insert(any()) } returns 42L

        val result = sut(newEvent(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { reproductionRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { reproductionRepositoryMock.update(any()) } returns 1L

        val result = sut(newEvent(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { reproductionRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { reproductionRepositoryMock.update(any()) }
    }
}
