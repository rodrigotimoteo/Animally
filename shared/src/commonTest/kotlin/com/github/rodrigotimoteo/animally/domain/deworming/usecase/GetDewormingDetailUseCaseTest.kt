package com.github.rodrigotimoteo.animally.domain.deworming.usecase

import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class GetDewormingDetailUseCaseTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private lateinit var sut: GetDewormingDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetDewormingDetailUseCase(dewormingRepositoryMock)
    }

    private fun newDeworming() =
        Deworming(
            id = 7L,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2024, 4, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when deworming exists then sut returns it`() {
        val deworming = newDeworming()

        every { dewormingRepositoryMock.getById(7L) } returns deworming

        val result = sut(7L)

        assertEquals(deworming, result)
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.getById(7L) }
    }

    @Test
    fun `when deworming does not exist then sut returns null`() {
        every { dewormingRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.getById(7L) }
    }
}
