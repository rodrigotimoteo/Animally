package com.github.rodrigotimoteo.animally.data.ultrasound

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class UltrasoundRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: UltrasoundRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = UltrasoundRepositoryImpl(database)
    }

    private fun newUltrasound(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate,
        follicleSizeMm: Double? = 25.0,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Ultrasound =
        Ultrasound(
            id = id,
            patientId = patientId,
            date = date,
            ovaryStatus = "Follicles present",
            uterineStatus = "Normal tone",
            follicleSizeMm = follicleSizeMm,
            findings = "No abnormalities",
            imageUris = "content://images/1",
            vetName = "Dr. Vet",
            notes = "Follow-up in 7 days",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no ultrasounds then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), result!!.date)
        assertEquals(25.0, result.follicleSizeMm)
        assertEquals("Follicles present", result.ovaryStatus)
        assertEquals("Normal tone", result.uterineStatus)
        assertEquals("content://images/1", result.imageUris)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 1, 10)))
        sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 6, 20)))
        sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 3, 15)))

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
        val id = sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 5, 1)))

        sut.update(
            newUltrasound(
                id = id,
                patientId = 1L,
                date = LocalDate(2024, 5, 1),
                follicleSizeMm = 32.5,
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals(32.5, sut.getById(id)!!.follicleSizeMm)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newUltrasound(patientId = 1L, date = LocalDate(2024, 5, 1)))
        sut.insert(newUltrasound(patientId = 2L, date = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
