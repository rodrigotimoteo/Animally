package com.github.rodrigotimoteo.animally.data.repromedication

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ReproMedicationRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: ReproMedicationRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = ReproMedicationRepositoryImpl(database)
    }

    private fun newReproMedication(
        id: Long = 0L,
        patientId: Long,
        medication: String = "Oxytocin",
        dateAdministered: LocalDate,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): ReproMedication =
        ReproMedication(
            id = id,
            patientId = patientId,
            medication = medication,
            dateAdministered = dateAdministered,
            dosage = "10 IU",
            purpose = "Induce labor",
            vetName = "Dr. Vet",
            notes = "Monitor closely",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no medications then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), result!!.dateAdministered)
        assertEquals("Oxytocin", result.medication)
        assertEquals("10 IU", result.dosage)
        assertEquals("Induce labor", result.purpose)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 1, 10)))
        sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 6, 20)))
        sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 3, 15)))

        val result = sut.getByPatient(1L)

        assertEquals(3, result.size)
        assertEquals(LocalDate(2024, 6, 20), result[0].dateAdministered)
        assertEquals(LocalDate(2024, 3, 15), result[1].dateAdministered)
        assertEquals(LocalDate(2024, 1, 10), result[2].dateAdministered)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 5, 1)))

        sut.update(
            newReproMedication(
                id = id,
                patientId = 1L,
                medication = "Flunixin",
                dateAdministered = LocalDate(2024, 5, 1),
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals("Flunixin", sut.getById(id)!!.medication)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newReproMedication(patientId = 1L, dateAdministered = LocalDate(2024, 5, 1)))
        sut.insert(newReproMedication(patientId = 2L, dateAdministered = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
