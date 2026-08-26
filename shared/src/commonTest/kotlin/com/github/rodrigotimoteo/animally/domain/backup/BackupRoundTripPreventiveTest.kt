package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for preventive care records:
 * vaccinations (incl. next-due), dewormings and dentistry.
 */
class BackupRoundTripPreventiveTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `vaccination round-trips batch site and next due date`() {
        seedPatient()
        database.vaccinationQueries.insertWithId(
            id = 90L,
            patientId = 1L,
            vaccineName = "Influenza",
            dateAdministered = LocalDate(2026, 4, 10),
            nextDueDate = LocalDate(2027, 4, 10),
            vetName = "Dr. Silva",
            batchNumber = "FLU-2026-8871",
            site = "Left neck",
            notes = "No adverse reaction",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(9000L),
            updatedAt = Instant.fromEpochMilliseconds(9100L),
        )
        database.vaccinationQueries.insertWithId(
            id = 91L,
            patientId = 1L,
            vaccineName = "Tetanus",
            dateAdministered = LocalDate(2025, 11, 2),
            nextDueDate = null,
            vetName = null,
            batchNumber = null,
            site = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(9200L),
            updatedAt = Instant.fromEpochMilliseconds(9300L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.vaccinationQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals("Influenza", restored[0].vaccineName)
        assertEquals(LocalDate(2026, 4, 10), restored[0].dateAdministered)
        assertEquals(LocalDate(2027, 4, 10), restored[0].nextDueDate)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals("FLU-2026-8871", restored[0].batchNumber)
        assertEquals("Left neck", restored[0].site)
        assertEquals("No adverse reaction", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].nextDueDate)
        assertEquals(null, restored[1].batchNumber)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `deworming round-trips product dose and next due date`() {
        seedPatient()
        database.dewormingQueries.insertWithId(
            id = 95L,
            patientId = 1L,
            product = "Ivermectin",
            dateAdministered = LocalDate(2026, 3, 1),
            nextDueDate = LocalDate(2026, 9, 1),
            dose = "200 mcg/kg",
            vetName = "Dr. Silva",
            notes = "Fecal egg count before treatment",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(9500L),
            updatedAt = Instant.fromEpochMilliseconds(9600L),
        )
        database.dewormingQueries.insertWithId(
            id = 96L,
            patientId = 1L,
            product = "Praziquantel",
            dateAdministered = LocalDate(2025, 12, 5),
            nextDueDate = null,
            dose = null,
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(9700L),
            updatedAt = Instant.fromEpochMilliseconds(9800L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.dewormingQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals("Ivermectin", restored[0].product)
        assertEquals(LocalDate(2026, 3, 1), restored[0].dateAdministered)
        assertEquals(LocalDate(2026, 9, 1), restored[0].nextDueDate)
        assertEquals("200 mcg/kg", restored[0].dose)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals("Fecal egg count before treatment", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].nextDueDate)
        assertEquals(null, restored[1].dose)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `dentistry round-trips findings treatment and next due date`() {
        seedPatient()
        database.dentistryQueries.insertWithId(
            id = 99L,
            patientId = 1L,
            date = LocalDate(2026, 2, 14),
            findings = "Sharp enamel points on buccal edges",
            treatment = "Full float with power grinder",
            nextDueDate = LocalDate(2026, 8, 14),
            vetName = "Dr. Costa",
            notes = "Sedated with detomidine",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(10000L),
            updatedAt = Instant.fromEpochMilliseconds(10100L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.dentistryQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(99L, restored.id)
        assertEquals(1L, restored.patientId)
        assertEquals(LocalDate(2026, 2, 14), restored.date)
        assertEquals("Sharp enamel points on buccal edges", restored.findings)
        assertEquals("Full float with power grinder", restored.treatment)
        assertEquals(LocalDate(2026, 8, 14), restored.nextDueDate)
        assertEquals("Dr. Costa", restored.vetName)
        assertEquals("Sedated with detomidine", restored.notes)
        assertEquals(true, restored.isActive)
        assertEquals(Instant.fromEpochMilliseconds(10000L), restored.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(10100L), restored.updatedAt)
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
