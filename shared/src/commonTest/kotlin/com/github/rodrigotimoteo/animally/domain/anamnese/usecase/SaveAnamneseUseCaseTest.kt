package com.github.rodrigotimoteo.animally.domain.anamnese.usecase

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveAnamneseUseCaseTest {
    /** Mock of [IAnamneseRepository] */
    private val anamneseRepositoryMock: IAnamneseRepository = mock()

    /** System under test [SaveAnamneseUseCase] */
    private lateinit var sut: SaveAnamneseUseCase

    @BeforeTest
    fun setup() {
        sut = SaveAnamneseUseCase(anamneseRepositoryMock)
    }

    private fun newAnamnese() =
        Anamnese(
            id = 1L,
            patientId = 7L,
            generalHistory = "History",
            chronicConditions = "Chronic",
            allergies = "None",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when saving then sut delegates to repository and returns its id`() {
        val anamnese = newAnamnese()

        every { anamneseRepositoryMock.save(any()) } returns 42L

        val result = sut(anamnese)

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { anamneseRepositoryMock.save(any()) }
    }
}
