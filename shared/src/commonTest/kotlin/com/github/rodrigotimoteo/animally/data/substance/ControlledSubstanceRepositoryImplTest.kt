package com.github.rodrigotimoteo.animally.data.substance

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ControlledSubstanceRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: ControlledSubstanceRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = ControlledSubstanceRepositoryImpl(database)
    }

    private fun newControlledSubstance(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate,
        drugName: String = "Xylazine",
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): ControlledSubstance =
        ControlledSubstance(
            id = id,
            patientId = patientId,
            drugName = drugName,
            dose = "1.5",
            unit = "mg",
            route = "IV",
            administeredBy = "Dr. Vet",
            witness = "Dr. Witness",
            date = date,
            reason = "Sedation for lameness exam",
            notes = "Log book entry 42",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no controlled substances then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then returns rows affected and retrieves by id`() {
        val id = sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), result!!.date)
        assertEquals("Xylazine", result.drugName)
        assertEquals("1.5", result.dose)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 1, 10)))
        sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 6, 20)))
        sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 3, 15)))

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
        val id = sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 5, 1)))

        sut.update(
            newControlledSubstance(
                id = id,
                patientId = 1L,
                date = LocalDate(2024, 5, 1),
                drugName = "Detomidine",
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        assertEquals("Detomidine", sut.getById(id)!!.drugName)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }

    @Test
    fun `when patient id differs then getByPatient returns only matching`() {
        sut.insert(newControlledSubstance(patientId = 1L, date = LocalDate(2024, 5, 1)))
        sut.insert(newControlledSubstance(patientId = 2L, date = LocalDate(2024, 5, 2)))

        assertEquals(1, sut.getByPatient(1L).size)
        assertEquals(1, sut.getByPatient(2L).size)
    }
}
