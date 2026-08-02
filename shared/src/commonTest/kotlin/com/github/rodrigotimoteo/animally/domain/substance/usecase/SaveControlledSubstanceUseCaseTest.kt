package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import dev.mokkery.answering.calls
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

class SaveControlledSubstanceUseCaseTest {
    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private lateinit var sut: SaveControlledSubstanceUseCase

    @BeforeTest
    fun setup() {
        sut = SaveControlledSubstanceUseCase(substanceRepositoryMock)
    }

    private fun newControlledSubstance(id: Long = 0L) =
        ControlledSubstance(
            id = id,
            patientId = 1L,
            drugName = "Xylazine",
            dose = "1.5",
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts`() {
        every { substanceRepositoryMock.insert(any()) } calls { 1L }

        val result = sut(newControlledSubstance())

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { substanceRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates`() {
        every { substanceRepositoryMock.update(any()) } calls { 1L }

        val result = sut(newControlledSubstance(id = 7L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { substanceRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.update(any()) }
    }
}
