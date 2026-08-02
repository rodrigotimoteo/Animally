package com.github.rodrigotimoteo.animally.data.farrier

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class FarrierVisitRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: FarrierVisitRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = FarrierVisitRepositoryImpl(database)
    }

    private fun newFarrierVisit(
        id: Long = 0L,
        patientId: Long,
        date: LocalDate = LocalDate(2024, 3, 1),
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): FarrierVisit =
        FarrierVisit(
            id = id,
            patientId = patientId,
            date = date,
            trimOrShoe = "Trim",
            shoeType = "Steel",
            findings = "Hooves balanced",
            nextDueDate = LocalDate(2024, 6, 1),
            farrier = "John Farrier",
            notes = "Crack on right front",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no farrier visits then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newFarrierVisit(patientId = 1L))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(LocalDate(2024, 3, 1), result!!.date)
        assertEquals("Trim", result.trimOrShoe)
        assertEquals("Steel", result.shoeType)
        assertEquals(LocalDate(2024, 6, 1), result.nextDueDate)
    }

    @Test
    fun `when getting by patient then returns only matching patient`() {
        sut.insert(newFarrierVisit(patientId = 1L, date = LocalDate(2024, 3, 1)))
        sut.insert(newFarrierVisit(patientId = 2L, date = LocalDate(2024, 3, 2)))

        val result = sut.getByPatient(1L)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2024, 3, 1), result.single().date)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newFarrierVisit(patientId = 1L))

        sut.update(newFarrierVisit(id = id, patientId = 1L, date = LocalDate(2024, 3, 1)).copy(shoeType = "Aluminum"))

        assertEquals("Aluminum", sut.getById(id)!!.shoeType)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newFarrierVisit(patientId = 1L))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
