package com.github.rodrigotimoteo.animally.data.insights

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.gestation.model.GestationStatus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Golden tests for Task 11 current gestation snapshot.
 *
 * Verifies gestation day recalculation, resolved/inactive exclusion, overdue explicit,
 * due-soon 30/60/90 inclusive groups, leap-date handling, case-insensitive statuses,
 * patient scope, inactive-patient exclusion, and source linkage.
 */
class InsightsGestationTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SqlDelightInsightsRepository

    private val now = Instant.fromEpochMilliseconds(0L)
    private val today = LocalDate(2025, 1, 31)

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        sut = SqlDelightInsightsRepository(database)
        seedPatients()
    }

    private fun seedPatients() {
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Star",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 2L,
            name = "Storm",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.patientQueries.insertWithId(
            id = 3L,
            name = "InactiveMare",
            species = "Equine",
            breed = null,
            dateOfBirth = null,
            gender = null,
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = false,
            createdAt = now,
            updatedAt = now,
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun insertGestation(
        id: Long,
        patientId: Long,
        breedingDate: LocalDate,
        status: String,
        isActive: Boolean = true,
        gestationDaysStored: Long = 999L,
        expectedDueDateStored: LocalDate =
            breedingDate.plus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS)),
    ) {
        database.gestationQueries.insertWithId(
            id = id,
            patientId = patientId,
            breedingDate = breedingDate,
            expectedDueDate = expectedDueDateStored,
            gestationDays = gestationDaysStored,
            status = status,
            fetalCount = null,
            lastCheckDate = null,
            notes = null,
            isActive = isActive,
            createdAt = now,
            updatedAt = now,
        )
    }

    @Test
    fun `gestation day recalculated while persisted expected due date is respected`() {
        // breeding 300 days before today -> gestationDay 300, due = breeding +340
        val breeding = today.minus(DatePeriod(days = 300))
        val expectedDue = breeding.plus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS))
        insertGestation(
            id = 1L,
            patientId = 1L,
            breedingDate = breeding,
            status = "Active",
            gestationDaysStored = 1L, // wrong stored value must be ignored
            expectedDueDateStored = expectedDue,
        )

        val snap = sut.getCurrentCareSnapshot(patientId = null, today = today)
        assertEquals(1, snap.activeGestations.size)
        val item = snap.activeGestations.single()
        assertEquals(300, item.gestationDay)
        assertEquals(expectedDue, item.dueDate)
        assertEquals(40, item.daysUntilDue) // 340-300=40
        assertEquals(1L, item.patientId)
        assertEquals("Star", item.patientName)
        assertEquals(1L, item.gestationId)
    }

    @Test
    fun `clinically adjusted expected due date drives due soon groups`() {
        val breeding = today.minus(DatePeriod(days = 200))
        val adjustedDue = today.plus(DatePeriod(days = 12))
        insertGestation(
            id = 11L,
            patientId = 1L,
            breedingDate = breeding,
            status = "Active",
            expectedDueDateStored = adjustedDue,
        )

        val snap = sut.getCurrentCareSnapshot(null, today)
        val item = snap.activeGestations.single()
        assertEquals(200, item.gestationDay)
        assertEquals(adjustedDue, item.dueDate)
        assertEquals(12, item.daysUntilDue)
        assertEquals(listOf(11L), snap.dueSoon30.map { it.gestationId })
    }

    @Test
    fun `inactive and resolved gestations excluded with case insensitive status`() {
        val breeding = today.minus(DatePeriod(days = 100))
        insertGestation(1L, 1L, breeding, "Active", isActive = false) // inactive
        insertGestation(2L, 1L, breeding, "Completed", isActive = true)
        insertGestation(3L, 1L, breeding, "completed", isActive = true)
        insertGestation(4L, 1L, breeding, "COMPLETED", isActive = true)
        insertGestation(5L, 1L, breeding, "Failed", isActive = true)
        insertGestation(6L, 1L, breeding, "failed", isActive = true)
        insertGestation(7L, 1L, breeding, "Foaled", isActive = true)
        insertGestation(8L, 1L, breeding, "FOALED", isActive = true)
        insertGestation(9L, 1L, breeding, "Active", isActive = true) // only this remains

        val snap = sut.getCurrentCareSnapshot(null, today)
        assertEquals(1, snap.activeGestations.size)
        assertEquals(9L, snap.activeGestations.single().gestationId)
    }

    @Test
    fun `overdue explicit and due soon groups inclusive boundaries`() {
        // today 2025-01-31
        // breeding dates chosen to produce daysUntilDue = -11, 0, 10, 29, 30, 31, 60, 61, 90, 91
        // due = breeding +340 => breeding = due -340 => breeding = (today + daysUntilDue) -340
        fun breedingFor(daysUntilDue: Int): LocalDate {
            val due = today.plus(DatePeriod(days = daysUntilDue))
            return due.minus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS))
        }

        val cases =
            listOf(
                -11 to 1L, // overdue
                0 to 2L, // due today
                10 to 3L,
                29 to 4L,
                30 to 5L, // boundary 30 inclusive
                31 to 6L,
                60 to 7L, // boundary 60
                61 to 8L,
                90 to 9L, // boundary 90
                91 to 10L,
            )
        for ((d, id) in cases) {
            insertGestation(id, 1L, breedingFor(d), "Active")
        }

        val snap = sut.getCurrentCareSnapshot(null, today)
        assertEquals(10, snap.activeGestations.size)

        // overdue excluded from dueSoon
        assertTrue(snap.dueSoon30.none { it.daysUntilDue < 0 })
        assertEquals(4, snap.dueSoon30.size) // 0,10,29,30
        assertEquals(setOf(2L, 3L, 4L, 5L), snap.dueSoon30.map { it.gestationId }.toSet())
        assertEquals(6, snap.dueSoon60.size) // +31 +60
        assertEquals(setOf(2L, 3L, 4L, 5L, 6L, 7L), snap.dueSoon60.map { it.gestationId }.toSet())
        assertEquals(8, snap.dueSoon90.size) // +61 +90
        assertEquals(setOf(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), snap.dueSoon90.map { it.gestationId }.toSet())

        // overdue still in active list with negative days
        val overdue = snap.activeGestations.first { it.gestationId == 1L }
        assertEquals(-11, overdue.daysUntilDue)
        assertTrue(overdue.daysUntilDue < 0)
    }

    @Test
    fun `gestation snapshot sorted by due date ascending`() {
        fun breedingFor(dueOffset: Int): LocalDate = today.plus(DatePeriod(days = dueOffset)).minus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS))
        insertGestation(1L, 1L, breedingFor(60), "Active")
        insertGestation(2L, 1L, breedingFor(10), "Active")
        insertGestation(3L, 1L, breedingFor(30), "Active")

        val snap = sut.getCurrentCareSnapshot(null, today)
        val orderedIds = snap.activeGestations.map { it.gestationId }
        assertEquals(listOf(2L, 3L, 1L), orderedIds) // 10,30,60
        // dueSoon also sorted because derived from same ordered list
        assertEquals(listOf(2L, 3L), snap.dueSoon30.map { it.gestationId })
    }

    @Test
    fun `each row links to mare and gestation source record`() {
        insertGestation(101L, 1L, today.minus(DatePeriod(days = 50)), "Active")
        insertGestation(102L, 2L, today.minus(DatePeriod(days = 60)), "Ongoing")

        val snap = sut.getCurrentCareSnapshot(null, today)
        assertEquals(2, snap.activeGestations.size)
        val m1 = snap.activeGestations.first { it.gestationId == 101L }
        assertEquals(1L, m1.patientId)
        assertEquals("Star", m1.patientName)
        assertEquals(101L, m1.gestationId)
        val m2 = snap.activeGestations.first { it.gestationId == 102L }
        assertEquals(2L, m2.patientId)
        assertEquals("Storm", m2.patientName)
        assertEquals(102L, m2.gestationId)
    }

    @Test
    fun `inactive patient rows excluded`() {
        val breeding = today.minus(DatePeriod(days = 100))
        insertGestation(1L, 3L, breeding, "Active", isActive = true) // patient 3 inactive
        insertGestation(2L, 1L, breeding, "Active", isActive = true)

        val snap = sut.getCurrentCareSnapshot(null, today)
        assertEquals(1, snap.activeGestations.size)
        assertEquals(2L, snap.activeGestations.single().gestationId)
    }

    @Test
    fun `patient scope filters gestations`() {
        val breeding = today.minus(DatePeriod(days = 100))
        insertGestation(1L, 1L, breeding, "Active")
        insertGestation(2L, 2L, breeding, "Active")

        val global = sut.getCurrentCareSnapshot(null, today)
        assertEquals(2, global.activeGestations.size)

        val p1 = sut.getCurrentCareSnapshot(1L, today)
        assertEquals(1, p1.activeGestations.size)
        assertEquals(1L, p1.activeGestations.single().patientId)

        val p2 = sut.getCurrentCareSnapshot(2L, today)
        assertEquals(1, p2.activeGestations.size)
        assertEquals(2L, p2.activeGestations.single().patientId)

        val none = sut.getCurrentCareSnapshot(999L, today)
        assertTrue(none.activeGestations.isEmpty())
    }

    @Test
    fun `leap date breeding recalculated correctly`() {
        // 2024 is leap year: breed 2024-02-29, due = 2025-02-02? 340 days after 2024-02-29
        val breeding = LocalDate(2024, 2, 29)
        val expectedDue = breeding.plus(DatePeriod(days = GestationStatus.GESTATION_PERIOD_DAYS))
        // today = 2025-01-31 -> gestationDay = breeding.daysUntil(today)
        val gestationDay = breeding.daysUntil(today)
        assertEquals(337, gestationDay) // 2024-02-29 to 2025-01-31 = 337 (leap correctness)
        insertGestation(1L, 1L, breeding, "Active")

        val snap = sut.getCurrentCareSnapshot(null, today)
        assertEquals(1, snap.activeGestations.size)
        val item = snap.activeGestations.single()
        assertEquals(gestationDay, item.gestationDay)
        assertEquals(expectedDue, item.dueDate)
        assertEquals(today.daysUntil(expectedDue), item.daysUntilDue)
    }

    @Test
    fun `today boundary and future breeding date yields gestationDay zeroNotNegative`() {
        // breeding tomorrow
        val breedingFuture = today.plus(DatePeriod(days = 1))
        insertGestation(1L, 1L, breedingFuture, "Active")

        val snap = sut.getCurrentCareSnapshot(null, today)
        val item = snap.activeGestations.single()
        assertEquals(0, item.gestationDay) // coerceAtLeast 0
        // due = breedingFuture +340 => today +341 => daysUntilDue 341
        assertEquals(341, item.daysUntilDue)
    }

    @Test
    fun `snapshot via use case integrates with period snapshot independently`() {
        // Ensure gestation snapshot does not affect overview counts but is present
        val breeding = today.minus(DatePeriod(days = 200))
        insertGestation(1L, 1L, breeding, "Active")
        // seed one consultation to ensure overview has activity
        database.consultationQueries.insertWithId(
            id = 100L,
            patientId = 1L,
            date = LocalDate(2025, 1, 15),
            subjective = "s",
            objective = "o",
            assessment = "a",
            plan = "p",
            vetName = null,
            nextVisitDate = null,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        val filter =
            com.github.rodrigotimoteo.animally.domain.insights.model
                .InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val useCase =
            com.github.rodrigotimoteo.animally.domain.insights.usecase
                .GetInsightsDashboardUseCase(sut) { today }
        val snap = useCase(filter)
        assertEquals(1, snap.currentCare.activeGestations.size)
        assertEquals(1, snap.overview.activityCount) // only consultation, gestation not counted as activity
    }
}
