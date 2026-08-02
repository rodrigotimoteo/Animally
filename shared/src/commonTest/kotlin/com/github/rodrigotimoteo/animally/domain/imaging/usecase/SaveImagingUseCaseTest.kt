package com.github.rodrigotimoteo.animally.domain.imaging.usecase

import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
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

class SaveImagingUseCaseTest {
    /** Mock of [IImagingRepository] */
    private val imagingRepositoryMock: IImagingRepository = mock()

    /** System under test [SaveImagingUseCase] */
    private lateinit var sut: SaveImagingUseCase

    @BeforeTest
    fun setup() {
        sut = SaveImagingUseCase(imagingRepositoryMock)
    }

    private fun imaging(id: Long): Imaging =
        Imaging(
            id = id,
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
    fun `when id equals zero then inserts and returns generated id`() {
        val imaging = imaging(id = 0L)

        every { imagingRepositoryMock.insert(imaging) } returns 7L

        val result = sut(imaging)

        assertEquals(expected = 7L, actual = result)
        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.insert(imaging) }
        verify(VerifyMode.exactly(0)) { imagingRepositoryMock.update(any()) }
    }

    @Test
    fun `when id differs from zero then updates and returns rows affected`() {
        val imaging = imaging(id = 5L)

        every { imagingRepositoryMock.update(imaging) } returns 1L

        val result = sut(imaging)

        assertEquals(expected = 1L, actual = result)
        verify(VerifyMode.exactly(1)) { imagingRepositoryMock.update(imaging) }
        verify(VerifyMode.exactly(0)) { imagingRepositoryMock.insert(any()) }
    }
}
