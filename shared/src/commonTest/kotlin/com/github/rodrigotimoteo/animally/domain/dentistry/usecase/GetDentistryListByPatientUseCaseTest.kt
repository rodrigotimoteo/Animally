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
import kotlin.time.Instant

class GetDentistryListByPatientUseCaseTest {
    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private lateinit var sut: GetDentistryListByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetDentistryListByPatientUseCase(dentistryRepositoryMock)
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
    fun `when repository returns dentistry records then sut returns them`() {
        val records = listOf(newDentistry(1L), newDentistry(2L))

        every { dentistryRepositoryMock.getByPatient(1L) } returns records

        val result = sut(1L)

        assertEquals(records, result)
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { dentistryRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(emptyList(), result)
        verify(VerifyMode.exactly(1)) { dentistryRepositoryMock.getByPatient(1L) }
    }
}
