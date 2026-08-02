package com.github.rodrigotimoteo.animally.domain.lameness.usecase

import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
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

class GetLamenessDetailUseCaseTest {
    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private lateinit var sut: GetLamenessDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetLamenessDetailUseCase(lamenessRepositoryMock)
    }

    private fun newLameness() =
        Lameness(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2024, 5, 1),
            gradeAAEP = 3,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when lameness exists then sut returns it`() {
        val lameness = newLameness()

        every { lamenessRepositoryMock.getById(7L) } returns lameness

        val result = sut(7L)

        assertEquals(lameness, result)
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.getById(7L) }
    }

    @Test
    fun `when lameness does not exist then sut returns null`() {
        every { lamenessRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { lamenessRepositoryMock.getById(7L) }
    }
}
