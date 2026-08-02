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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetLabResultDetailUseCaseTest {
    /** Mock of [ILabResultRepository] */
    private val labResultRepositoryMock: ILabResultRepository = mock()

    /** System under test [GetLabResultDetailUseCase] */
    private lateinit var sut: GetLabResultDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetLabResultDetailUseCase(labResultRepositoryMock)
    }

    private fun labResult(): LabResult =
        LabResult(
            id = 1L,
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
    fun `when repository returns result then sut returns it`() {
        val labResult = labResult()

        every { labResultRepositoryMock.getById(1L) } returns labResult

        val result = sut(1L)

        assertEquals(expected = labResult, actual = result)
        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getById(1L) }
    }

    @Test
    fun `when repository returns null then sut returns null`() {
        every { labResultRepositoryMock.getById(999L) } returns null

        assertNull(sut(999L))

        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getById(999L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { labResultRepositoryMock.getById(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.getById(1L) }
    }
}
