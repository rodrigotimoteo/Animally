package com.github.rodrigotimoteo.animally.data.vaccination

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class VaccinationRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: VaccinationRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = VaccinationRepositoryImpl(database)
    }

    private fun newVaccination(
        id: Long = 0L,
        patientId: Long,
        vaccineName: String = "Tetanus",
        nextDueDate: LocalDate? = LocalDate(2025, 1, 15),
        isActive: Boolean = true,
    ): Vaccination =
        Vaccination(
            id = id,
            patientId = patientId,
            vaccineName = vaccineName,
            dateAdministered = LocalDate(2024, 1, 15),
            nextDueDate = nextDueDate,
            vetName = "Dr. Vet",
            batchNumber = "B-123",
            site = "Neck",
            notes = "No reaction",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

    @Test
    fun `when patient has no vaccinations then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newVaccination(patientId = 1L))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals("Tetanus", result!!.vaccineName)
        assertEquals(LocalDate(2024, 1, 15), result.dateAdministered)
        assertEquals(LocalDate(2025, 1, 15), result.nextDueDate)
        assertEquals("B-123", result.batchNumber)
        assertEquals("No reaction", result.notes)
    }

    @Test
    fun `when getting by patient then returns only matching patient`() {
        sut.insert(newVaccination(patientId = 1L, vaccineName = "Tetanus"))
        sut.insert(newVaccination(patientId = 2L, vaccineName = "Influenza"))

        val result = sut.getByPatient(1L)

        assertEquals(1, result.size)
        assertEquals("Tetanus", result.single().vaccineName)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newVaccination(patientId = 1L))

        sut.update(
            newVaccination(
                id = id,
                patientId = 1L,
                vaccineName = "Influenza",
                nextDueDate = LocalDate(2024, 7, 15),
            ),
        )

        val result = sut.getById(id)
        assertEquals("Influenza", result!!.vaccineName)
        assertEquals(LocalDate(2024, 7, 15), result.nextDueDate)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newVaccination(patientId = 1L))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
