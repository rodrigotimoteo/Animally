package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
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

class GetControlledSubstanceDetailUseCaseTest {
    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private lateinit var sut: GetControlledSubstanceDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetControlledSubstanceDetailUseCase(substanceRepositoryMock)
    }

    private fun newControlledSubstance() =
        ControlledSubstance(
            id = 7L,
            patientId = 1L,
            drugName = "Xylazine",
            dose = "1.5",
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when controlled substance exists then sut returns it`() {
        val substance = newControlledSubstance()

        every { substanceRepositoryMock.getById(7L) } returns substance

        val result = sut(7L)

        assertEquals(substance, result)
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.getById(7L) }
    }

    @Test
    fun `when controlled substance does not exist then sut returns null`() {
        every { substanceRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.getById(7L) }
    }
}
