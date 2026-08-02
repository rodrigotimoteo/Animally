package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class DeletePatientUseCaseTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private lateinit var sut: DeletePatientUseCase

    @BeforeTest
    fun setup() {
        sut = DeletePatientUseCase(patientRepositoryMock)
    }

    @Test
    fun `when patient has active records then throws PatientHasRecordsException`() {
        every { patientRepositoryMock.countActiveRecords(1L) } returns 3L

        val exception = assertFailsWith<PatientHasRecordsException> { sut(1L) }

        assertEquals(3L, exception.recordCount)
        assertEquals("Patient has 3 records. Delete records first or use soft delete.", exception.message)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countActiveRecords(1L) }
        verify(VerifyMode.exactly(0)) { patientRepositoryMock.setInactive(1L, any()) }
    }

    @Test
    fun `when patient has no records then marks patient inactive`() {
        every { patientRepositoryMock.countActiveRecords(1L) } returns 0L
        every { patientRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L

        sut(1L)

        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countActiveRecords(1L) }
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.setInactive(1L, any<Instant>()) }
    }
}
