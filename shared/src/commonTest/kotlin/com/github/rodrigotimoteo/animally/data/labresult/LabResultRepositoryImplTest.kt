package com.github.rodrigotimoteo.animally.data.labresult

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class LabResultRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: LabResultRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = LabResultRepositoryImpl(database)
    }

    private fun newLabResult(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate,
        testType: String = "CBC",
        results: String? = "12.5",
        normalRange: String? = "5.0-15.0",
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): LabResult =
        LabResult(
            id = id,
            patientId = patientId,
            testType = testType,
            date = date,
            results = results,
            normalRange = normalRange,
            vetName = "Dr. Vet",
            notes = null,
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no lab results then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then returns rows affected and retrieves by id`() {
        val id = sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), assertNotNull(result).date)
        assertEquals("CBC", result.testType)
        assertEquals("12.5", result.results)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 1, 10)))
        sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 6, 20)))
        sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 3, 15)))

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
        val id = sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 5, 1)))

        sut.update(
            newLabResult(
                id = id,
                patientId = 1L,
                date = LocalDate(2024, 5, 1),
                testType = "Chemistry Panel",
                results = "85",
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals("Chemistry Panel", assertNotNull(sut.getById(id)).testType)
        assertEquals("85", assertNotNull(sut.getById(id)).results)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newLabResult(patientId = 1L, date = LocalDate(2024, 5, 1)))
        sut.insert(newLabResult(patientId = 2L, date = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
