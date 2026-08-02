package com.github.rodrigotimoteo.animally.domain.anamnese.usecase

import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class GetAnamneseByPatientUseCaseTest {
    /** Mock of [IAnamneseRepository] */
    private val anamneseRepositoryMock: IAnamneseRepository = mock()

    /** System under test [GetAnamneseByPatientUseCase] */
    private lateinit var sut: GetAnamneseByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetAnamneseByPatientUseCase(anamneseRepositoryMock)
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
    fun `when anamnese exists then sut returns it`() {
        val anamnese = newAnamnese()

        every { anamneseRepositoryMock.getByPatient(7L) } returns anamnese

        val result = sut(7L)

        assertEquals(anamnese, result)
        verify(VerifyMode.exactly(1)) { anamneseRepositoryMock.getByPatient(7L) }
    }

    @Test
    fun `when patient has no anamnese then sut returns null`() {
        every { anamneseRepositoryMock.getByPatient(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { anamneseRepositoryMock.getByPatient(7L) }
    }
}
