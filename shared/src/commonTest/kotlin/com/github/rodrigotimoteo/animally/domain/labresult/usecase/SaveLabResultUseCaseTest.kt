package com.github.rodrigotimoteo.animally.domain.labresult.usecase

import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
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

class SaveLabResultUseCaseTest {
    /** Mock of [ILabResultRepository] */
    private val labResultRepositoryMock: ILabResultRepository = mock()

    /** System under test [SaveLabResultUseCase] */
    private lateinit var sut: SaveLabResultUseCase

    @BeforeTest
    fun setup() {
        sut = SaveLabResultUseCase(labResultRepositoryMock)
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
    fun `when id equals zero then inserts and returns generated id`() {
        val labResult = labResult(id = 0L)

        every { labResultRepositoryMock.insert(labResult) } returns 7L

        val result = sut(labResult)

        assertEquals(expected = 7L, actual = result)
        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.insert(labResult) }
        verify(VerifyMode.exactly(0)) { labResultRepositoryMock.update(any()) }
    }

    @Test
    fun `when id differs from zero then updates and returns rows affected`() {
        val labResult = labResult(id = 5L)

        every { labResultRepositoryMock.update(labResult) } returns 1L

        val result = sut(labResult)

        assertEquals(expected = 1L, actual = result)
        verify(VerifyMode.exactly(1)) { labResultRepositoryMock.update(labResult) }
        verify(VerifyMode.exactly(0)) { labResultRepositoryMock.insert(any()) }
    }
}
