package com.github.rodrigotimoteo.animally.data.patient

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class PatientRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: PatientRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = PatientRepositoryImpl(database)
    }

    private fun newPatient(
        id: Long = 0L,
        name: String,
        species: String = "Equine",
        ownerId: Long? = null,
        isActive: Boolean = true,
        createdAt: Instant = Instant.fromEpochMilliseconds(0L),
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Patient =
        Patient(
            id = id,
            name = name,
            species = species,
            ownerId = ownerId,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    @Test
    fun `when database is empty then returns empty list`() {
        assertEquals(emptyList(), sut.getPatientList())
    }

    @Test
    fun `when inserting patient then returns rows affected and lists it`() {
        val result = sut.insertPatient(newPatient(name = "Charlie"))

        assertEquals(1L, result)
        assertEquals(1, sut.getPatientList().size)
        assertEquals("Charlie", sut.getPatientList().single().name)
    }

    @Test
    fun `when inserting multiple patients then produces different ids`() {
        sut.insertPatient(newPatient(name = "Alpha"))
        sut.insertPatient(newPatient(name = "Beta", createdAt = Instant.fromEpochMilliseconds(1L), updatedAt = Instant.fromEpochMilliseconds(1L)))

        val result = sut.getPatientList()
        assertEquals(2, result.size)
        assertNotEquals(result[0].id, result[1].id)
    }

    @Test
    fun `when patient exists then returns patient by id`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))

        val result = sut.getPatientById(id)

        assertNotNull(result)
        assertEquals("Charlie", result!!.name)
    }

    @Test
    fun `when patient does not exist then returns null`() {
        assertNull(sut.getPatientById(999L))
    }

    @Test
    fun `when updating patient then modifies existing fields`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))

        sut.updatePatient(newPatient(id = id, name = "Charlie Updated", updatedAt = Instant.fromEpochMilliseconds(200L)))

        with(sut.getPatientById(id)!!) {
            assertEquals("Charlie Updated", name)
            assertEquals("Equine", species)
        }
    }

    @Test
    fun `when updating non-existent patient then returns zero rows affected`() {
        val result = sut.updatePatient(newPatient(id = 999L, name = "Ghost"))

        assertEquals(0L, result)
    }

    @Test
    fun `when patient is inactive then excludes from queries`() {
        sut.insertPatient(newPatient(name = "Charlie", isActive = false))

        assertEquals(emptyList(), sut.getPatientList())
        assertNull(sut.getPatientById(1L))
    }

    @Test
    fun `when setting inactive then removes patient from list`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getPatientById(id))
        assertEquals(emptyList(), sut.getPatientList())
    }

    @Test
    fun `when setting inactive on non-existent patient then returns zero rows affected`() {
        val result = sut.setInactive(999L, Instant.fromEpochMilliseconds(0L))

        assertEquals(0L, result)
    }

    @Test
    fun `when patient has no linked records then countActiveRecords returns zero`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))

        assertEquals(0L, sut.countActiveRecords(id))
    }

    @Test
    fun `when patient has linked records then countActiveRecords counts them`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))
        database.weightQueries.insert(
            patientId = id,
            weightKg = 520.0,
            date = LocalDate(2024, 1, 1),
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        database.vaccinationQueries.insert(
            patientId = id,
            vaccineName = "Flu",
            dateAdministered = LocalDate(2024, 1, 1),
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

        val result = sut.countActiveRecords(id)

        assertEquals(2L, result)
        assertTrue(result > 0)
    }

    @Test
    fun `when linked record is inactive then countActiveRecords excludes it`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))
        database.weightQueries.insert(
            patientId = id,
            weightKg = 520.0,
            date = LocalDate(2024, 1, 1),
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

        assertEquals(0L, sut.countActiveRecords(id))
    }

    @Test
    fun `when patient has no owner then getPatientsByOwnerId returns empty`() {
        val id = sut.insertPatient(newPatient(name = "Charlie"))

        assertEquals(emptyList(), sut.getPatientsByOwnerId(42L))
        assertEquals(0L, sut.countPatientsByOwnerId(42L))
        assertEquals("Charlie", sut.getPatientById(id)!!.name)
    }
}
