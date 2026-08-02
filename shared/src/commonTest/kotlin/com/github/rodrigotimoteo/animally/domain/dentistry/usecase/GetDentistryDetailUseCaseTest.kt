package com.github.rodrigotimoteo.animally.domain.dentistry.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
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

class GetDentistryDetailUseCaseTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private lateinit var sut: GetDentistryDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetDentistryDetailUseCase(dentistryRepositoryMock)
    }

    private fun newDentistry() =
        Dentistry(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2024, 6, 1),
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when dentistry record exists then sut returns it`() {
        val record = newDentistry()

        every { dentistryRepositoryMock.getById(7L) } returns record

        val result = sut(7L)

        assertEquals(record, result)
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.getById(7L) }
    }

    @Test
    fun `when dentistry record does not exist then sut returns null`() {
        every { dentistryRepositoryMock.getById(7L) } returns null

        val result = sut(7L)

        assertNull(result)
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.getById(7L) }
    }
}
