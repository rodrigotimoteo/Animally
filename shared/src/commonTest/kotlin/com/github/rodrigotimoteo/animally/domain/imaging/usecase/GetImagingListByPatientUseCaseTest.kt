package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
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

class GetImagingListByPatientUseCaseTest {
    /** Mock of [IImagingRepository] */
    private val imagingRepositoryMock: IImagingRepository = mock()

    /** System under test [GetImagingListByPatientUseCase] */
    private lateinit var sut: GetImagingListByPatientUseCase

    @BeforeTest
    fun setup() {
        sut = GetImagingListByPatientUseCase(imagingRepositoryMock)
    }

    private fun imaging(id: Long): Imaging =
        Imaging(
            id = id,
            patientId = 1L,
            type = "X-ray",
            date = LocalDate(2024, 5, 1),
            findings = "No abnormalities",
            imageUris = "/img/a.png,/img/b.png",
            vetName = "Dr. Vet",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns list then sut returns the same list`() {
        val imagingRecords = listOf(imaging(1L), imaging(2L))

        every { imagingRepositoryMock.getByPatient(1L) } returns imagingRecords

        val result = sut(1L)

        assertEquals(expected = imagingRecords, actual = result)
        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository returns empty list then sut returns empty list`() {
        every { imagingRepositoryMock.getByPatient(1L) } returns emptyList()

        val result = sut(1L)

        assertEquals(expected = emptyList<Imaging>(), actual = result)
        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getByPatient(1L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { imagingRepositoryMock.getByPatient(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getByPatient(1L) }
    }
}
