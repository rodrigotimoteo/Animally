package com.github.rodrigotimoteo.animally.data.lameness

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class LamenessRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: LamenessRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = LamenessRepositoryImpl(database)
    }

    private fun newLameness(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate,
        gradeAAEP: Int = 3,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Lameness =
        Lameness(
            id = id,
            patientId = patientId,
            date = date,
            gradeAAEP = gradeAAEP,
            limbLocation = "Right fore",
            flexionTest = "Positive",
            diagnosis = "Suspected tendonitis",
            treatment = "Rest and NSAIDs",
            vetName = "Dr. Vet",
            notes = "Follow up in 2 weeks",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no lameness records then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then returns rows affected and retrieves by id`() {
        val id = sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), assertNotNull(result).date)
        assertEquals(3, result.gradeAAEP)
        assertEquals("Suspected tendonitis", result.diagnosis)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 1, 10)))
        sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 6, 20)))
        sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 3, 15)))

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
        val id = sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 5, 1)))

        sut.update(
            newLameness(
                id = id,
                patientId = 1L,
                date = LocalDate(2024, 5, 1),
                gradeAAEP = 4,
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals(4, assertNotNull(sut.getById(id)).gradeAAEP)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newLameness(patientId = 1L, date = LocalDate(2024, 5, 1)))
        sut.insert(newLameness(patientId = 2L, date = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
