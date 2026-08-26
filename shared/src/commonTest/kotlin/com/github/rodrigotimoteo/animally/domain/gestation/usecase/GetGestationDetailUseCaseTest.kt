package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetGestationDetailUseCaseTest {
    /** Mock of [IGestationRepository] */
    private val gestationRepositoryMock: IGestationRepository = mock()

    /** System under test [GetGestationDetailUseCase] */
    private lateinit var sut: GetGestationDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetGestationDetailUseCase(gestationRepositoryMock, CalculateGestationUseCase())
    }

    private fun gestation() =
        Gestation(
            id = 7L,
            patientId = 1L,
            breedingDate = LocalDate(2024, 3, 1),
            expectedDueDate = LocalDate(2025, 2, 4),
            gestationDays = 120,
            status = "In Progress",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns active gestation then sut refreshes derived progress`() {
        val gestation = gestation()

        every { gestationRepositoryMock.getById(any()) } returns gestation

        val result = sut(7L, LocalDate(2024, 3, 31))

        assertEquals(30, result?.gestationDays)
        assertEquals(gestation.expectedDueDate, result?.expectedDueDate)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getById(any()) }
    }

    @Test
    fun `when repository finds nothing then sut returns null`() {
        every { gestationRepositoryMock.getById(any()) } returns null

        val result = sut(7L, LocalDate(2024, 3, 31))

        assertNull(result)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.getById(any()) }
    }
}
