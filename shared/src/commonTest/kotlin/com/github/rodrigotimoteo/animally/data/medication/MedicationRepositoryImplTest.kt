package com.github.rodrigotimoteo.animally.data.medication

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class MedicationRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: MedicationRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = MedicationRepositoryImpl(database)
    }

    private fun newMedication(
        id: Long = 0L,
        patientId: Long,
        name: String = "Phenylbutazone",
        dosage: String = "2g",
        startDate: LocalDate? = null,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Medication =
        Medication(
            id = id,
            patientId = patientId,
            name = name,
            dosage = dosage,
            route = "Oral",
            frequency = "Once daily",
            startDate = startDate,
            endDate = null,
            prescribedBy = "Dr. Vet",
            notes = "With food",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no medications then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then returns rows affected and retrieves by id`() {
        val id = sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals("Phenylbutazone", assertNotNull(result).name)
        assertEquals("2g", result.dosage)
        assertEquals(LocalDate(2024, 5, 1), result.startDate)
    }

    @Test
    fun `when getting by patient then returns list sorted by start date descending`() {
        sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 1, 10)))
        sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 6, 20)))
        sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 3, 15)))

        val result = sut.getByPatient(1L)

        assertEquals(3, result.size)
        assertEquals(LocalDate(2024, 6, 20), result[0].startDate)
        assertEquals(LocalDate(2024, 3, 15), result[1].startDate)
        assertEquals(LocalDate(2024, 1, 10), result[2].startDate)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 5, 1)))

        sut.update(
            newMedication(
                id = id,
                patientId = 1L,
                name = "Flunixin",
                startDate = LocalDate(2024, 5, 1),
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals("Flunixin", assertNotNull(sut.getById(id)).name)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newMedication(patientId = 1L, startDate = LocalDate(2024, 5, 1)))
        sut.insert(newMedication(patientId = 2L, startDate = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
