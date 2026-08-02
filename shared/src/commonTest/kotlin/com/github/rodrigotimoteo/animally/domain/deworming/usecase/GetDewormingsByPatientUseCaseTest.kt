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
import kotlin.time.Instant

class GetDewormingsByPatientUseCaseTest {
    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private lateinit var sut: GetDewormingsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetDewormingsByPatientUseCase(dewormingRepositoryMock)
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
    fun `when repository returns dewormings then sut returns them`() {
        val dewormings = listOf(newDeworming(1L), newDeworming(2L))

        every { dewormingRepositoryMock.getByPatient(1L) } returns dewormings

        val result = sut(1L)

        assertEquals(dewormings, result)
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { dewormingRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList(), result)
        verify(VerifyMode.exactly(1)) { dewormingRepositoryMock.getByPatient(1L) }
    }
}
