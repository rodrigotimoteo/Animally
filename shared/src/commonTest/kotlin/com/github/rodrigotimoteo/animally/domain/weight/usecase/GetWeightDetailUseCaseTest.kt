package com.github.rodrigotimoteo.animally.domain.weight.usecase

import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
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

class GetWeightDetailUseCaseTest {
    private val weightRepositoryMock: IWeightRepository = mock()

    private lateinit var sut: GetWeightDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetWeightDetailUseCase(weightRepositoryMock)
    }

    private fun newWeight() =
        Weight(
            id = 7L,
            patientId = 1L,
            weightKg = 520.0,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when weight exists then sut returns it`() {
        val weight = newWeight()

        every { weightRepositoryMock.getById(7L) } returns weight

        val result = sut(7L)

        assertEquals(weight, result)
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.getById(7L) }
    }

    @Test
    fun `when weight does not exist then sut returns null`() {
        every { weightRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.getById(7L) }
    }
}
