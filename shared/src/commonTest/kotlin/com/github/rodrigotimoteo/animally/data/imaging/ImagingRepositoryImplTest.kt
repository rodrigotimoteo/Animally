package com.github.rodrigotimoteo.animally.data.imaging

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ImagingRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: ImagingRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = ImagingRepositoryImpl(database)
    }

    private fun newImaging(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate,
        type: String = "X-ray",
        findings: String? = "No abnormalities",
        imageUris: String? = "/img/a.png,/img/b.png",
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Imaging =
        Imaging(
            id = id,
            patientId = patientId,
            type = type,
            date = date,
            findings = findings,
            imageUris = imageUris,
            vetName = "Dr. Vet",
            notes = null,
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no imaging records then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then returns rows affected and retrieves by id`() {
        val id = sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), assertNotNull(result).date)
        assertEquals("X-ray", result.type)
        assertEquals("No abnormalities", result.findings)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 1, 10)))
        sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 6, 20)))
        sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 3, 15)))

        val result = sut.getByPatient(1L)

        assertEquals(3, result.size)
        assertEquals(LocalDate(2024, 6, 20), result[0].date)
        assertEquals(LocalDate(2024, 3, 15), result[1].date)
        assertEquals(LocalDate(2024, 1, 10), result[2].date)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 5, 1)))

        sut.update(
            newImaging(
                id = id,
                patientId = 1L,
                date = LocalDate(2024, 5, 1),
                type = "MRI",
                findings = "Soft tissue lesion",
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals("MRI", assertNotNull(sut.getById(id)).type)
        assertEquals("Soft tissue lesion", assertNotNull(sut.getById(id)).findings)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newImaging(patientId = 1L, date = LocalDate(2024, 5, 1)))
        sut.insert(newImaging(patientId = 2L, date = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
