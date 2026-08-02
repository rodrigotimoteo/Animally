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
import kotlin.test.assertNull
import kotlin.time.Instant

class GetImagingDetailUseCaseTest {
    /** Mock of [IImagingRepository] */
    private val imagingRepositoryMock: IImagingRepository = mock()

    /** System under test [GetImagingDetailUseCase] */
    private lateinit var sut: GetImagingDetailUseCase

    @BeforeTest
    fun setup() {
        sut = GetImagingDetailUseCase(imagingRepositoryMock)
    }

    private fun imaging(): Imaging =
        Imaging(
            id = 1L,
            patientId = 1L,
            type = "X-ray",
            date = LocalDate(2024, 5, 1),
            findings = "No abnormalities",
            imageUris = "/img/a.png",
            vetName = "Dr. Vet",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when repository returns record then sut returns it`() {
        val imaging = imaging()

        every { imagingRepositoryMock.getById(1L) } returns imaging

        val result = sut(1L)

        assertEquals(expected = imaging, actual = result)
        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getById(1L) }
    }

    @Test
    fun `when repository returns null then sut returns null`() {
        every { imagingRepositoryMock.getById(999L) } returns null

        assertNull(sut(999L))

        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getById(999L) }
    }

    @Test
    fun `when repository throws then sut propagates exception`() {
        every { imagingRepositoryMock.getById(1L) } throws RuntimeException("boom")

        assertFailsWith<RuntimeException> { sut(1L) }

        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.getById(1L) }
    }
}
