package com.github.rodrigotimoteo.animally.data.deworming

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DewormingRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: DewormingRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = DewormingRepositoryImpl(database)
    }

    private fun newDeworming(
        id: Long = 0L,
        patientId: Long,
        product: String = "Ivermectin",
        dateAdministered: LocalDate = LocalDate(2024, 4, 1),
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): Deworming =
        Deworming(
            id = id,
            patientId = patientId,
            product = product,
            dateAdministered = dateAdministered,
            nextDueDate = LocalDate(2024, 10, 1),
            dose = "1 tube",
            vetName = "Dr. Vet",
            notes = "Weight-based dose",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no deworming records then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newDeworming(patientId = 1L))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals("Ivermectin", result!!.product)
        assertEquals(LocalDate(2024, 4, 1), result.dateAdministered)
        assertEquals(LocalDate(2024, 10, 1), result.nextDueDate)
        assertEquals("1 tube", result.dose)
    }

    @Test
    fun `when getting by patient then returns only matching patient`() {
        sut.insert(newDeworming(patientId = 1L, product = "Ivermectin"))
        sut.insert(newDeworming(patientId = 2L, product = "Praziquantel"))

        val result = sut.getByPatient(1L)

        assertEquals(1, result.size)
        assertEquals("Ivermectin", result.single().product)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newDeworming(patientId = 1L))

        sut.update(newDeworming(id = id, patientId = 1L, product = "Praziquantel"))

        assertEquals("Praziquantel", sut.getById(id)!!.product)
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newDeworming(patientId = 1L))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
    }
}
