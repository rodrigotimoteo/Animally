package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
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

class SaveLamenessUseCaseTest {
    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private lateinit var sut: SaveLamenessUseCase

    @BeforeTest
    fun setup() {
        sut = SaveLamenessUseCase(lamenessRepositoryMock)
    }

    private fun newLameness(id: Long = 0L) =
        Lameness(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            gradeAAEP = 3,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts`() {
        every { lamenessRepositoryMock.insert(any()) } calls { 1L }

        val result = sut(newLameness())

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { lamenessRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates`() {
        every { lamenessRepositoryMock.update(any()) } calls { 1L }

        val result = sut(newLameness(id = 7L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { lamenessRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.update(any()) }
    }
}
