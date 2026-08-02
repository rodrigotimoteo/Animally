package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
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

class SaveSurgeryUseCaseTest {
    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private lateinit var sut: SaveSurgeryUseCase

    @BeforeTest
    fun setup() {
        sut = SaveSurgeryUseCase(surgeryRepositoryMock)
    }

    private fun newSurgery(id: Long = 0L) =
        Surgery(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts`() {
        every { surgeryRepositoryMock.insert(any()) } calls { 1L }

        val result = sut(newSurgery())

        assertEquals(1L, result)
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { surgeryRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates`() {
        every { surgeryRepositoryMock.update(any()) } calls { 1L }

        val result = sut(newSurgery(id = 7L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { surgeryRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.update(any()) }
    }
}
