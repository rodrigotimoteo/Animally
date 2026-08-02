package com.github.rodrigotimoteo.animally.domain.surgery.usecase

import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class GetSurgeriesByPatientUseCaseTest {
    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private lateinit var sut: GetSurgeriesByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetSurgeriesByPatientUseCase(surgeryRepositoryMock)
    }

    private fun newSurgery(id: Long) =
        Surgery(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns list then sut returns the same list`() {
        val surgeries = listOf(newSurgery(1L), newSurgery(2L))

        every { surgeryRepositoryMock.getByPatient(1L) } returns surgeries

        val result = sut(1L)

        assertEquals(surgeries, result)
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { surgeryRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList<Surgery>(), result)
        verify(VerifyMode.exactly(1)) { surgeryRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { surgeryRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }
    }
}
