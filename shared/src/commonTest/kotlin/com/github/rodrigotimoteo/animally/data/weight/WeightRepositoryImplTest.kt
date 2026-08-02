package com.github.rodrigotimoteo.animally.data.weight

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class WeightRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: WeightRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = WeightRepositoryImpl(database)
    }

    private fun newWeight(
        id: Long = 0L,
        patientId: Long,
        weightKg: Double,
        date: LocalDate,
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Weight =
        Weight(
            id = id,
            patientId = patientId,
            weightKg = weightKg,
            date = date,
            notes = "routine weigh-in",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no weight entries then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newWeight(patientId = 1L, weightKg = 520.0, date = LocalDate(2024, 5, 1)))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals(520.0, assertNotNull(result).weightKg)
        assertEquals(LocalDate(2024, 5, 1), result.date)
        assertEquals("routine weigh-in", result.notes)
    }

    @Test
    fun `when getting by patient then returns list sorted by date descending`() {
        sut.insert(newWeight(patientId = 1L, weightKg = 500.0, date = LocalDate(2024, 1, 10)))
        sut.insert(newWeight(patientId = 1L, weightKg = 520.0, date = LocalDate(2024, 6, 20)))
        sut.insert(newWeight(patientId = 1L, weightKg = 510.0, date = LocalDate(2024, 3, 15)))

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
        val id = sut.insert(newWeight(patientId = 1L, weightKg = 500.0, date = LocalDate(2024, 5, 1)))

        sut.update(newWeight(id = id, patientId = 1L, weightKg = 515.0, date = LocalDate(2024, 5, 1)))

        assertEquals(515.0, assertNotNull(sut.getById(id)).weightKg)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newWeight(patientId = 1L, weightKg = 500.0, date = LocalDate(2024, 5, 1)))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
