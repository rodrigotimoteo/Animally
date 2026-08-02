package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetGestationsByPatientUseCaseTest {
    /** Mock of [IGestationRepository] */
    private val gestationRepositoryMock: IGestationRepository = mock()

    /** System under test [GetGestationsByPatientUseCase] */
    private lateinit var sut: GetGestationsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetGestationsByPatientUseCase(gestationRepositoryMock)
    }

    private fun gestation(
        id: Long,
        breedingDate: LocalDate,
    ) = Gestation(
        id = id,
        patientId = 1L,
        breedingDate = breedingDate,
        expectedDueDate = breedingDate,
        gestationDays = 0,
        status = "In Progress",
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `when repository returns gestations then sut returns the same list`() {
        val gestations =
            listOf(
                gestation(id = 1L, breedingDate = LocalDate(2024, 3, 1)),
                gestation(id = 2L, breedingDate = LocalDate(2024, 2, 1)),
            )

        every { gestationRepositoryMock.getByPatient(any()) } returns gestations

        val result = sut(1L)

        assertEquals(expected = gestations, actual = result)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val gestations = emptyList<Gestation>()

        every { gestationRepositoryMock.getByPatient(any()) } returns gestations

        val result = sut(1L)

        assertEquals(expected = gestations, actual = result)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { gestationRepositoryMock.getByPatient(any()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getByPatient(any()) }
    }
}
