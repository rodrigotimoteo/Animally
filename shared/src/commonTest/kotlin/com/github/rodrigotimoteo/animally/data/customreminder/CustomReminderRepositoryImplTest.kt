package com.github.rodrigotimoteo.animally.data.customreminder

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class CustomReminderRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: CustomReminderRepositoryImpl

    private val today = LocalDate(2025, 1, 15)

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = CustomReminderRepositoryImpl(database)
    }

    private fun newReminder(
        id: Long = 0L,
        patientId: Long,
        dueDate: LocalDate = LocalDate(2025, 2, 1),
        isActive: Boolean = true,
        updatedAt: Instant = Instant.fromEpochMilliseconds(0L),
    ): CustomReminder =
        CustomReminder(
            id = id,
            patientId = patientId,
            title = "Farrier check",
            dueDate = dueDate,
            linkedRecordType = "FarrierVisit",
            linkedRecordId = 7L,
            notes = "Call ahead",
            isActive = isActive,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = updatedAt,
        )

    @Test
    fun `when patient has no reminders then returns empty list`() {
        assertEquals(emptyList(), sut.getByPatient(999L))
    }

    @Test
    fun `when inserting then round-trips all fields`() {
        val id = sut.insert(newReminder(patientId = 1L))

        assertEquals(1L, id)
        val result = sut.getById(id)
        assertNotNull(result)
        assertEquals("Farrier check", result.title)
        assertEquals(LocalDate(2025, 2, 1), result.dueDate)
        assertEquals("FarrierVisit", result.linkedRecordType)
        assertEquals(7L, result.linkedRecordId)
        assertEquals("Call ahead", result.notes)
        assertEquals(true, result.isActive)
    }

    @Test
    fun `when getting by patient then returns only matching patient`() {
        sut.insert(newReminder(patientId = 1L, dueDate = LocalDate(2025, 2, 1)))
        sut.insert(newReminder(patientId = 2L, dueDate = LocalDate(2025, 2, 2)))

        val result = sut.getByPatient(1L)

        assertEquals(1, result.size)
        assertEquals(LocalDate(2025, 2, 1), result.single().dueDate)
    }

    @Test
    fun `when getting by non-existent id then returns null`() {
        assertNull(sut.getById(999L))
    }

    @Test
    fun `when updating then modifies existing fields`() {
        val id = sut.insert(newReminder(patientId = 1L))

        sut.update(newReminder(id = id, patientId = 1L).copy(title = "Shoeing"))

        assertEquals("Shoeing", assertNotNull(sut.getById(id)).title)
    }

    @Test
    fun `when getting upcoming then returns active reminders due on or after today ordered by due date`() {
        sut.insert(newReminder(patientId = 1L, dueDate = LocalDate(2025, 1, 14)))
        sut.insert(newReminder(patientId = 1L, dueDate = LocalDate(2025, 1, 15)))
        sut.insert(newReminder(patientId = 2L, dueDate = LocalDate(2025, 3, 1)))

        val result = sut.getUpcoming(today)

        assertEquals(
            listOf(LocalDate(2025, 1, 15), LocalDate(2025, 3, 1)),
            result.map { it.dueDate },
        )
    }

    @Test
    fun `when getting overdue then returns active reminders due before today`() {
        sut.insert(newReminder(patientId = 1L, dueDate = LocalDate(2025, 1, 10)))
        sut.insert(newReminder(patientId = 1L, dueDate = LocalDate(2025, 1, 15)))

        val result = sut.getOverdue(today)

        assertEquals(listOf(LocalDate(2025, 1, 10)), result.map { it.dueDate })
    }

    @Test
    fun `when setting inactive then excludes from queries`() {
        val id = sut.insert(newReminder(patientId = 1L))

        val result = sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertEquals(1L, result)
        assertNull(sut.getById(id))
        assertEquals(emptyList(), sut.getByPatient(1L))
        assertEquals(emptyList(), sut.getUpcoming(today))
    }
}
