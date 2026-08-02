package com.github.rodrigotimoteo.animally.domain.ultrasound.usecase

import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
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

class GetUltrasoundsByPatientUseCaseTest {
    /** Mock of [IUltrasoundRepository] */
    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    /** System under test [GetUltrasoundsByPatientUseCase] */
    private lateinit var sut: GetUltrasoundsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetUltrasoundsByPatientUseCase(ultrasoundRepositoryMock)
    }

    private fun ultrasound(
        id: Long,
        date: LocalDate,
    ) = Ultrasound(
        id = id,
        patientId = 1L,
        date = date,
        follicleSizeMm = 35.0,
        createdAt = Instant.fromEpochMilliseconds(0L),
        updatedAt = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `when repository returns ultrasounds then sut returns the same list`() {
        val ultrasounds =
            listOf(
                ultrasound(id = 1L, date = LocalDate(2024, 3, 1)),
                ultrasound(id = 2L, date = LocalDate(2024, 2, 1)),
            )

        every { ultrasoundRepositoryMock.getByPatient(any()) } returns ultrasounds

        val result = sut(1L)

        assertEquals(expected = ultrasounds, actual = result)
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        val ultrasounds = emptyList<Ultrasound>()

        every { ultrasoundRepositoryMock.getByPatient(any()) } returns ultrasounds

        val result = sut(1L)

        assertEquals(expected = ultrasounds, actual = result)
        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.getByPatient(any()) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { ultrasoundRepositoryMock.getByPatient(any()) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { ultrasoundRepositoryMock.getByPatient(any()) }
    }
}
