package com.github.rodrigotimoteo.animally.domain.weight.usecase

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
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

class SaveWeightUseCaseTest {
    private val weightRepositoryMock: IWeightRepository = mock()

    private lateinit var sut: SaveWeightUseCase

    @BeforeTest
    fun setup() {
        sut = SaveWeightUseCase(weightRepositoryMock, FakeSearchRepository())
    }

    private fun newWeight(id: Long) =
        Weight(
            id = id,
            patientId = 1L,
            weightKg = 500.0,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { weightRepositoryMock.insert(any()) } returns 42L

        val result = sut(newWeight(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { weightRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { weightRepositoryMock.update(any()) } returns 1L

        val result = sut(newWeight(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { weightRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.update(any()) }
    }
}
