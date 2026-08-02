package com.github.rodrigotimoteo.animally.domain.substance.usecase

import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetControlledSubstancesByPatientUseCaseTest {
    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private lateinit var sut: GetControlledSubstancesByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetControlledSubstancesByPatientUseCase(substanceRepositoryMock)
    }

    private fun newControlledSubstance(id: Long) =
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
    fun `when repository returns list then sut returns the same list`() {
        val substances = listOf(newControlledSubstance(1L), newControlledSubstance(2L))

        every { substanceRepositoryMock.getByPatient(1L) } returns substances

        val result = sut(1L)

        assertEquals(substances, result)
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { substanceRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList<ControlledSubstance>(), result)
        verify(VerifyMode.exactly(1)) { substanceRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { substanceRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }
    }
}
