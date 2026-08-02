package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetUltrasoundDetailUseCaseTest {
    /** Mock of [IUltrasoundRepository] */
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    /** System under test [GetUltrasoundDetailUseCase] */
    private lateinit var sut: GetUltrasoundDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetUltrasoundDetailUseCase(ultrasoundRepositoryMock)
    }

    private fun ultrasound() =
        Ultrasound(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2024, 6, 1),
            ovaryStatus = "Normal",
            uterineStatus = "Normal",
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns ultrasound then sut returns it`() {
        val ultrasound = ultrasound()

        every { ultrasoundRepositoryMock.getById(any()) } returns ultrasound

        val result = sut(7L)

        assertEquals(expected = ultrasound, actual = result)
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.getById(any()) }
    }

    @Test
    fun `when repository finds nothing then sut returns null`() {
        every { ultrasoundRepositoryMock.getById(any()) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.getById(any()) }
    }
}
