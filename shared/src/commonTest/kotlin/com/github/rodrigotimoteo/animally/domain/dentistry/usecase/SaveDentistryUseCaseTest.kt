package com.github.rodrigotimoteo.animally.domain.dentistry.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
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

class SaveDentistryUseCaseTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private lateinit var sut: SaveDentistryUseCase

    @BeforeTest
    fun setup() {
        sut = SaveDentistryUseCase(dentistryRepositoryMock)
    }

    private fun newDentistry(id: Long) =
        Dentistry(
            id = id,
            patientId = 1L,
            date = LocalDate(2024, 6, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when id is zero then sut inserts and returns generated id`() {
        every { dentistryRepositoryMock.insert(any()) } returns 42L

        val result = sut(newDentistry(id = 0L))

        assertEquals(42L, result)
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(0)) { dentistryRepositoryMock.update(any()) }
    }

    @Test
    fun `when id is non-zero then sut updates and returns rows affected`() {
        every { dentistryRepositoryMock.update(any()) } returns 1L

        val result = sut(newDentistry(id = 5L))

        assertEquals(1L, result)
        verify(VerifyMode.exactly(0)) { dentistryRepositoryMock.insert(any()) }
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.update(any()) }
    }
}
