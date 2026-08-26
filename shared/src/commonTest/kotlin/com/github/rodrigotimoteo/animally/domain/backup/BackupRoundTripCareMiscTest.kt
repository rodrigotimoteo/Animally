package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for farrier visits, custom reminders
 * (regression: custom reminders were previously absent from the backup) and
 * the weight series with decimal precision.
 */
class BackupRoundTripCareMiscTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `farrier visit round-trips trim shoe type and next due date`() {
        seedPatient()
        database.farrierVisitQueries.insertWithId(
            id = 105L,
            patientId = 1L,
            date = LocalDate(2026, 5, 8),
            trimOrShoe = "Shoeing",
            shoeType = "Full set - steel",
            findings = "Moderate toe wear",
            nextDueDate = LocalDate(2026, 8, 8),
            farrier = "Marcos",
            notes = "Check hind clips next visit",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(10500L),
            updatedAt = Instant.fromEpochMilliseconds(10600L),
        )
        database.farrierVisitQueries.insertWithId(
            id = 106L,
            patientId = 1L,
            date = LocalDate(2026, 2, 8),
            trimOrShoe = null,
            shoeType = null,
            findings = null,
            nextDueDate = null,
            farrier = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(10700L),
            updatedAt = Instant.fromEpochMilliseconds(10800L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.farrierVisitQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals("Shoeing", restored[0].trimOrShoe)
        assertEquals("Full set - steel", restored[0].shoeType)
        assertEquals("Moderate toe wear", restored[0].findings)
        assertEquals(LocalDate(2026, 8, 8), restored[0].nextDueDate)
        assertEquals("Marcos", restored[0].farrier)
        assertEquals("Check hind clips next visit", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].trimOrShoe)
        assertEquals(null, restored[1].nextDueDate)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `custom reminder round-trips linked record fields and soft-delete`() {
        seedPatient()
        database.customReminderQueries.insertWithId(
            id = 110L,
            patientId = 1L,
            title = "Annual check",
            dueDate = LocalDate(2026, 10, 20),
            linkedRecordType = "Vaccination",
            linkedRecordId = 90L,
            notes = "Book with Dr. Silva",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(11000L),
            updatedAt = Instant.fromEpochMilliseconds(11100L),
        )
        database.customReminderQueries.insertWithId(
            id = 111L,
            patientId = 1L,
            title = "Recheck hooves",
            dueDate = LocalDate(2026, 9, 1),
            linkedRecordType = null,
            linkedRecordId = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(11200L),
            updatedAt = Instant.fromEpochMilliseconds(11300L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.customReminderQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(110L, restored[0].id)
        assertEquals(1L, restored[0].patientId)
        assertEquals("Annual check", restored[0].title)
        assertEquals(LocalDate(2026, 10, 20), restored[0].dueDate)
        assertEquals("Vaccination", restored[0].linkedRecordType)
        assertEquals(90L, restored[0].linkedRecordId)
        assertEquals("Book with Dr. Silva", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(Instant.fromEpochMilliseconds(11000L), restored[0].createdAt)
        assertEquals(Instant.fromEpochMilliseconds(11100L), restored[0].updatedAt)
        assertEquals(111L, restored[1].id)
        assertEquals(null, restored[1].linkedRecordType)
        assertEquals(null, restored[1].linkedRecordId)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `weight series round-trips decimal precision across multiple entries`() {
        seedPatient()
        val weights = listOf(520.0, 487.25, 512.125)
        weights.forEachIndexed { index, kg ->
            database.weightQueries.insertWithId(
                id = 120L + index,
                patientId = 1L,
                weightKg = kg,
                date = LocalDate(2026, 4, 1 + index),
                notes = if (index == 1) "After deworming" else null,
                isActive = index != 2,
                createdAt = Instant.fromEpochMilliseconds((12000 + index * 10).toLong()),
                updatedAt = Instant.fromEpochMilliseconds((12500 + index * 10).toLong()),
            )
        }

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.weightQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(3, restored.size)
        weights.forEachIndexed { index, kg ->
            assertEquals(120L + index, restored[index].id)
            assertEquals(kg, restored[index].weightKg)
            assertEquals(LocalDate(2026, 4, 1 + index), restored[index].date)
            assertEquals(if (index == 1) "After deworming" else null, restored[index].notes)
            assertEquals(index != 2, restored[index].isActive)
            assertEquals(Instant.fromEpochMilliseconds((12000 + index * 10).toLong()), restored[index].createdAt)
        }
    }

    private fun seedPatient() {
        database.patientQueries.insertWithId(
            id = 1L,
            name = "Charlie",
            species = "Equine",
            breed = "Hanoverian",
            dateOfBirth = LocalDate(2018, 3, 1),
            gender = "Mare",
            microchipId = null,
            ueln = null,
            registrationNumber = null,
            stableLocation = null,
            photoUri = null,
            notes = null,
            ownerId = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(300L),
            updatedAt = Instant.fromEpochMilliseconds(400L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun exportAndWipe(): String {
        var content: String? = null
        ExportBackupUseCase(
            database = database,
            writeFile = { fileName, json ->
                content = json
                "backups/$fileName"
            },
            copyDatabase = { "backups/animally.db" },
        ).invoke()
        val json = requireNotNull(content)
        database.deleteAllBackupRows()
        return json
    }
}
