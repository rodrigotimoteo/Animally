package com.github.rodrigotimoteo.animally.data.gestation

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class GestationRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: GestationRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = GestationRepositoryImpl(database)
    }

    private fun newGestation(
        id: Long = 0L,
        patientId: Long,
        breedingDate: LocalDate,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Gestation =
        Gestation(
            id = id,
            patientId = patientId,
            breedingDate = breedingDate,
            expectedDueDate = breedingDate.plus(DatePeriod(days = 340)),
            gestationDays = 0,
            status = "Active",
            fetalCount = 1,
            lastCheckDate = breedingDate,
            notes = "Healthy pregnancy",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no gestations then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newGestation(patientId = 1L, breedingDate = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 5, 1), assertNotNull(result).breedingDate)
        assertEquals(LocalDate(2025, 4, 6), result.expectedDueDate)
        assertEquals("Active", result.status)
        assertEquals(1, result.fetalCount)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newGestation(patientId = 1L, breedingDate = LocalDate(2024, 5, 1)))

        sut.update(
            newGestation(
                id = id,
                patientId = 1L,
                breedingDate = LocalDate(2024, 5, 1),
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ).copy(
                status = "Confirmed",
                gestationDays = 100,
            ),
        )

        val result = sut.getById(id)
        assertEquals("Confirmed", assertNotNull(result).status)
        assertEquals(100, result.gestationDays)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newGestation(patientId = 1L, breedingDate = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
