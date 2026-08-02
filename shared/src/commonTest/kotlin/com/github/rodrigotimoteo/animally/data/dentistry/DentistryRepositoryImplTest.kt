package com.github.rodrigotimoteo.animally.data.dentistry

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DentistryRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: DentistryRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = DentistryRepositoryImpl(database)
    }

    private fun newDentistry(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate = LocalDate(2024, 6, 1),
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Dentistry =
        Dentistry(
            id = id,
            patientId = patientId,
            date = date,
            findings = "Sharp points",
            treatment = "Floating performed",
            nextDueDate = LocalDate(2024, 9, 1),
            vetName = "Dr. Vet",
            notes = "Mild sedation used",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no dentistry records then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newDentistry(patientId = 1L))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 6, 1), assertNotNull(result).date)
        assertEquals("Sharp points", result.findings)
        assertEquals("Floating performed", result.treatment)
        assertEquals(LocalDate(2024, 9, 1), result.nextDueDate)
    }

    @Test
    fun `when getting by patient then returns only matching patient`() {
        sut.insert(newDentistry(patientId = 1L, date = LocalDate(2024, 6, 1)))
        sut.insert(newDentistry(patientId = 2L, date = LocalDate(2024, 6, 2)))

        val result = sut.getByPatient(1L)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2024, 6, 1), result.single().date)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newDentistry(patientId = 1L))

        sut.update(newDentistry(id = id, patientId = 1L, date = LocalDate(2024, 6, 1)).copy(treatment = "Wolf teeth removed"))

        assertEquals("Wolf teeth removed", assertNotNull(sut.getById(id)).treatment)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newDentistry(patientId = 1L))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
