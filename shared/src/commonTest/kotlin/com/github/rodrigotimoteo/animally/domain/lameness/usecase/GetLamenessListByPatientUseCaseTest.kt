package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
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

class GetLamenessListByPatientUseCaseTest {
    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private lateinit var sut: GetLamenessListByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetLamenessListByPatientUseCase(lamenessRepositoryMock)
    }

    private fun newLameness(id: Long) =
        Lameness(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            gradeAAEP = 3,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns list then sut returns the same list`() {
        val lamenesses = listOf(newLameness(1L), newLameness(2L))

        every { lamenessRepositoryMock.getByPatient(1L) } returns lamenesses

        val result = sut(1L)

        assertEquals(lamenesses, result)
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { lamenessRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList<Lameness>(), result)
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { lamenessRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }
    }
}
