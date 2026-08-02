package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
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

class GetSurgeryDetailUseCaseTest {
    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private lateinit var sut: GetSurgeryDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetSurgeryDetailUseCase(surgeryRepositoryMock)
    }

    private fun newSurgery() =
        Surgery(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when surgery exists then sut returns it`() {
        val surgery = newSurgery()

        every { surgeryRepositoryMock.getById(7L) } returns surgery

        val result = sut(7L)

        assertEquals(surgery, result)
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.getById(7L) }
    }

    @Test
    fun `when surgery does not exist then sut returns null`() {
        every { surgeryRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.getById(7L) }
    }
}
