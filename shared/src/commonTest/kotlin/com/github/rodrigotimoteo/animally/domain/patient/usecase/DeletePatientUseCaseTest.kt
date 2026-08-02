package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import dev.mokkery.MockMode
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

    private val searchRepositoryMock: ISearchRepository = mock(MockMode.autoUnit)

    private lateinit var sut: DeletePatientUseCase

    @BeforeTest
    fun setup() {
        sut = DeletePatientUseCase(patientRepositoryMock, searchRepositoryMock)
    }

    @Test
    fun `when patient has active records then throws PatientHasRecordsException`() {
        every { patientRepositoryMock.countActiveRecords(1L) } returns 3L

        val exception = assertFailsWith<PatientHasRecordsException> { sut(1L) }

        assertEquals(3L, exception.recordCount)
        assertEquals("Patient has 3 records. Delete records first or use soft delete.", exception.message)
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countActiveRecords(1L) }
        verify(VerifyMode.exactly(0)) { patientRepositoryMock.setInactive(1L, any()) }
        verify(VerifyMode.exactly(0)) { searchRepositoryMock.deleteRecord(ISearchRepository.TYPE_PATIENT, 1L) }
    }

    @Test
    fun `when patient has no records then marks patient inactive and removes from index`() {
        every { patientRepositoryMock.countActiveRecords(1L) } returns 0L
        every { patientRepositoryMock.setInactive(1L, any<Instant>()) } returns 1L

        sut(1L)

        verify(VerifyMode.exactly(1)) { patientRepositoryMock.countActiveRecords(1L) }
        verify(VerifyMode.exactly(1)) { patientRepositoryMock.setInactive(1L, any<Instant>()) }
        verify(VerifyMode.exactly(1)) { searchRepositoryMock.deleteRecord(ISearchRepository.TYPE_PATIENT, 1L) }
    }
}
