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
import kotlin.time.Instant

class GetWeightsByPatientUseCaseTest {
    private val weightRepositoryMock: IWeightRepository = mock()

    private lateinit var sut: GetWeightsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetWeightsByPatientUseCase(weightRepositoryMock)
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
    fun `when repository returns weights then sut returns them`() {
        val weights = listOf(newWeight(1L), newWeight(2L))

        every { weightRepositoryMock.getByPatient(1L) } returns weights

        val result = sut(1L)

        assertEquals(weights, result)
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { weightRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList(), result)
        verify(VerifyMode.exactly(1)) { weightRepositoryMock.getByPatient(1L) }
    }
}
