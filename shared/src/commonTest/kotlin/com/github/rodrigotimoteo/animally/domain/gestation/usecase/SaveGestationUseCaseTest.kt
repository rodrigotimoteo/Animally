package com.github.rodrigotimoteo.animally.domain.gestation.usecase

import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SaveGestationUseCaseTest {
    private val gestationRepositoryMock: IGestationRepository = mock()

    private lateinit var sut: SaveGestationUseCase

    @BeforeTest
    fun setup() {
        sut = SaveGestationUseCase(gestationRepositoryMock, CalculateGestationUseCase())
    }

    private val breedingDate = LocalDate(2024, 3, 1)
    private val today = breedingDate.plus(DatePeriod(days = 100))

    private fun newGestation(id: Long) =
        Gestation(
            id = id,
            patientId = 1L,
            breedingDate = breedingDate,
            expectedDueDate = LocalDate(2024, 1, 1),
            gestationDays = 0,
            status = "In Progress",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut computes progress and inserts`() {
        val captured = mutableListOf<Gestation>()
        every { gestationRepositoryMock.insert(any()) } calls { args ->
            captured += args.arg<Gestation>(0)
            42L
        }

        val result = sut(newGestation(id = 0L), today)

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { gestationRepositoryMock.update(any()) }
        assertEquals(breedingDate.plus(DatePeriod(days = 340)), captured.single().expectedDueDate)
        assertEquals(100, captured.single().gestationDays)
    }

    @Test
    fun `when id is non-zero then sut computes progress and updates`() {
        val captured = mutableListOf<Gestation>()
        every { gestationRepositoryMock.update(any()) } calls { args ->
            captured += args.arg<Gestation>(0)
            1L
        }

        val result = sut(newGestation(id = 5L), today)

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { gestationRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { gestationRepositoryMock.update(any()) }
        assertEquals(breedingDate.plus(DatePeriod(days = 340)), captured.single().expectedDueDate)
        assertEquals(100, captured.single().gestationDays)
    }

    @Test
    fun `when today precedes breeding then gestation days is zero`() {
        val captured = mutableListOf<Gestation>()
        every { gestationRepositoryMock.update(any()) } calls { args ->
            captured += args.arg<Gestation>(0)
            1L
        }

        sut(newGestation(id = 5L), breedingDate.plus(DatePeriod(days = -10)))

        assertEquals(0, captured.single().gestationDays)
    }
}
