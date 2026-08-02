package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
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

class GetLabResultsByPatientUseCaseTest {
    /** Mock of [ILabResultRepository] */
    private val labResultRepositoryMock: ILabResultRepository = mock()

    /** System under test [GetLabResultsByPatientUseCase] */
    private lateinit var sut: GetLabResultsByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetLabResultsByPatientUseCase(labResultRepositoryMock)
    }

    private fun labResult(id: Long): LabResult =
        LabResult(
            id = id,
            patientId = 1L,
            testType = "CBC",
            date = LocalDate(2024, 5, 1),
            results = "12.5",
            normalRange = "5.0-15.0",
            vetName = "Dr. Vet",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns list then sut returns the same list`() {
        val labResults = listOf(labResult(1L), labResult(2L))

        every { labResultRepositoryMock.getByPatient(1L) } returns labResults

        val result = sut(1L)

        assertEquals(expected = labResults, actual = result)
        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { labResultRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(expected = emptyList<LabResult>(), actual = result)
        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { labResultRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getByPatient(1L) }
    }
}
