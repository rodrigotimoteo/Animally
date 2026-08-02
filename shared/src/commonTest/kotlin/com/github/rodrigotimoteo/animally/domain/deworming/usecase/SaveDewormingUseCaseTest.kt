package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
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
import kotlin.time.Instant

class SaveDewormingUseCaseTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private lateinit var sut: SaveDewormingUseCase

    @BeforeTest
    fun setup() {
        sut = SaveDewormingUseCase(dewormingRepositoryMock)
    }

    private fun newDeworming(id: Long) =
        Deworming(
            id = id,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2024, 4, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { dewormingRepositoryMock.insert(any()) } returns 42L

        val result = sut(newDeworming(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { dewormingRepositoryMock.update(any()) } returns 1L

        val result = sut(newDeworming(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { dewormingRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.update(any()) }
    }
}
