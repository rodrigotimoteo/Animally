package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

import com.github.rodrigotimoteo.animally.domain.search.FakeSearchRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
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

class SaveUltrasoundUseCaseTest {
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    private lateinit var sut: SaveUltrasoundUseCase

    @BeforeTest
    fun setup() {
        sut = SaveUltrasoundUseCase(ultrasoundRepositoryMock, FakeSearchRepository())
    }

    private fun newUltrasound(id: Long) =
        Ultrasound(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 3, 1),
            follicleSizeMm = 30.0,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { ultrasoundRepositoryMock.insert(any()) } returns 42L

        val result = sut(newUltrasound(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { ultrasoundRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { ultrasoundRepositoryMock.update(any()) } returns 1L

        val result = sut(newUltrasound(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { ultrasoundRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.update(any()) }
    }
}
