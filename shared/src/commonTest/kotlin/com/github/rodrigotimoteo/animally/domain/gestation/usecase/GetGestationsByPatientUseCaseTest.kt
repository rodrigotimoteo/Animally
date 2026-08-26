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
        sut = GetGestationsByPatientUseCase(gestationRepositoryMock, CalculateGestationUseCase())
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
    fun `when repository returns active gestations then sut refreshes derived progress`() {
        val gestations =
            listOf(
                gestation(id = 1L, breedingDate = LocalDate(2024, 3, 1)),
                gestation(id = 2L, breedingDate = LocalDate(2024, 2, 1)),
            )
        val today = LocalDate(2024, 3, 10)

        every { gestationRepositoryMock.getByPatient(any()) } returns gestations

        val result = sut(1L, today)

        assertEquals(9, result[0].gestationDays)
        assertEquals(38, result[1].gestationDays)
        assertEquals(LocalDate(2025, 2, 4), result[0].expectedDueDate)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository returns resolved gestation then sut preserves recorded progress`() {
        val completed =
            gestation(id = 1L, breedingDate = LocalDate(2024, 3, 1)).copy(
                expectedDueDate = LocalDate(2025, 2, 4),
                gestationDays = 120,
                status = "Completed",
            )
        every { gestationRepositoryMock.getByPatient(any()) } returns listOf(completed)

        val result = sut(1L, LocalDate(2026, 8, 26))

        assertEquals(completed, result.single())
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
