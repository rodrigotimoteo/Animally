package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for the reproduction cycle family:
 * reproduction events (incl. stallion/breeding fields) and gestations.
 */
class BackupRoundTripReproCycleTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `reproduction event round-trips stallion breeding type and exam findings`() {
        seedPatient()
        database.reproductionQueries.insertWithId(
            id = 31L,
            patientId = 1L,
            eventType = "Breeding",
            date = LocalDate(2026, 7, 2),
            details = "Third cover of the cycle",
            vetName = "Dr. Silva",
            notes = "Mare in good condition",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1300L),
            updatedAt = Instant.fromEpochMilliseconds(2300L),
            initialExamFindings = "Good uterine tone, 35mm follicle",
            stallionName = "Cassiano",
            breedingType = "Natural cover",
        )
        database.reproductionQueries.insertWithId(
            id = 32L,
            patientId = 1L,
            eventType = "Heat",
            date = LocalDate(2026, 7, 1),
            details = null,
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(1350L),
            updatedAt = Instant.fromEpochMilliseconds(2350L),
            initialExamFindings = null,
            stallionName = null,
            breedingType = null,
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.reproductionQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(31L, restored[0].id)
        assertEquals("Breeding", restored[0].eventType)
        assertEquals(LocalDate(2026, 7, 2), restored[0].date)
        assertEquals("Third cover of the cycle", restored[0].details)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals("Mare in good condition", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals("Good uterine tone, 35mm follicle", restored[0].initialExamFindings)
        assertEquals("Cassiano", restored[0].stallionName)
        assertEquals("Natural cover", restored[0].breedingType)
        assertEquals(Instant.fromEpochMilliseconds(1300L), restored[0].createdAt)
        assertEquals(Instant.fromEpochMilliseconds(2300L), restored[0].updatedAt)
        assertEquals("Heat", restored[1].eventType)
        assertEquals(false, restored[1].isActive)
        assertEquals(null, restored[1].stallionName)
        assertEquals(null, restored[1].breedingType)
        assertEquals(null, restored[1].initialExamFindings)
    }

    @Test
    fun `gestation round-trips due date day count fetal count and check date`() {
        seedPatient()
        database.gestationQueries.insertWithId(
            id = 61L,
            patientId = 1L,
            breedingDate = LocalDate(2026, 7, 2),
            expectedDueDate = LocalDate(2027, 6, 6),
            gestationDays = 120L,
            status = "In foal",
            fetalCount = 1L,
            lastCheckDate = LocalDate(2026, 10, 30),
            notes = "Single viable fetus",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(6000L),
            updatedAt = Instant.fromEpochMilliseconds(6100L),
        )
        database.gestationQueries.insertWithId(
            id = 62L,
            patientId = 1L,
            breedingDate = LocalDate(2025, 8, 10),
            expectedDueDate = LocalDate(2026, 7, 14),
            gestationDays = 340L,
            status = null,
            fetalCount = null,
            lastCheckDate = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(6200L),
            updatedAt = Instant.fromEpochMilliseconds(6300L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.gestationQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(61L, restored[0].id)
        assertEquals(1L, restored[0].patientId)
        assertEquals(LocalDate(2026, 7, 2), restored[0].breedingDate)
        assertEquals(LocalDate(2027, 6, 6), restored[0].expectedDueDate)
        assertEquals(120L, restored[0].gestationDays)
        assertEquals("In foal", restored[0].status)
        assertEquals(1L, restored[0].fetalCount)
        assertEquals(LocalDate(2026, 10, 30), restored[0].lastCheckDate)
        assertEquals("Single viable fetus", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].status)
        assertEquals(null, restored[1].fetalCount)
        assertEquals(null, restored[1].lastCheckDate)
        assertEquals(false, restored[1].isActive)
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
