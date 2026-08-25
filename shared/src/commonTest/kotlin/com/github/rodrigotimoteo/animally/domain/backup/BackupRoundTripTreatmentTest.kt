package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for clinical treatment records:
 * medication, controlled substances and surgeries.
 */
class BackupRoundTripTreatmentTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `medication round-trips dosage route schedule and nullable dates`() {
        seedPatient()
        database.medicationQueries.insertWithId(
            id = 30L,
            patientId = 1L,
            name = "Phenylbutazone",
            dosage = "2 g",
            route = "Oral",
            frequency = "Every 12 h",
            startDate = LocalDate(2026, 6, 15),
            endDate = LocalDate(2026, 6, 29),
            prescribedBy = "Dr. Silva",
            notes = "Give with food",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(900L),
            updatedAt = Instant.fromEpochMilliseconds(950L),
        )
        database.medicationQueries.insertWithId(
            id = 31L,
            patientId = 1L,
            name = "Omeprazole",
            dosage = "1 g",
            route = null,
            frequency = null,
            startDate = null,
            endDate = null,
            prescribedBy = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(960L),
            updatedAt = Instant.fromEpochMilliseconds(970L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.medicationQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(30L, restored[0].id)
        assertEquals("Phenylbutazone", restored[0].name)
        assertEquals("2 g", restored[0].dosage)
        assertEquals("Oral", restored[0].route)
        assertEquals("Every 12 h", restored[0].frequency)
        assertEquals(LocalDate(2026, 6, 15), restored[0].startDate)
        assertEquals(LocalDate(2026, 6, 29), restored[0].endDate)
        assertEquals("Dr. Silva", restored[0].prescribedBy)
        assertEquals("Give with food", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].startDate)
        assertEquals(null, restored[1].endDate)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `controlled substance round-trips dose administrator and witness`() {
        seedPatient()
        database.substanceQueries.insertWithId(
            id = 40L,
            patientId = 1L,
            drugName = "Detomidine",
            dose = "0.02",
            unit = "mg/kg",
            route = "IV",
            administeredBy = "Dr. Silva",
            witness = "Nurse Ana",
            date = LocalDate(2026, 6, 15),
            reason = "Sedation for floating",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1000L),
            updatedAt = Instant.fromEpochMilliseconds(1100L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.substanceQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(40L, restored.id)
        assertEquals(1L, restored.patientId)
        assertEquals("Detomidine", restored.drugName)
        assertEquals("0.02", restored.dose)
        assertEquals("mg/kg", restored.unit)
        assertEquals("IV", restored.route)
        assertEquals("Dr. Silva", restored.administeredBy)
        assertEquals("Nurse Ana", restored.witness)
        assertEquals(LocalDate(2026, 6, 15), restored.date)
        assertEquals("Sedation for floating", restored.reason)
        assertEquals(null, restored.notes)
        assertEquals(true, restored.isActive)
        assertEquals(Instant.fromEpochMilliseconds(1000L), restored.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(1100L), restored.updatedAt)
    }

    @Test
    fun `surgery round-trips anesthesia analgesia complications and recovery`() {
        seedPatient()
        database.surgeryQueries.insertWithId(
            id = 80L,
            patientId = 1L,
            date = LocalDate(2026, 5, 2),
            type = "Arthroscopy",
            description = "Chip removal from left front fetlock",
            outcome = "Excellent",
            surgeon = "Dr. Costa",
            anesthesia = "General - isoflurane",
            analgesia = "Flunixin meglumine",
            complications = "Transient post-op swelling",
            recoveryNotes = "Sutures out day 12, hand walking week 3",
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(5000L),
            updatedAt = Instant.fromEpochMilliseconds(5100L),
        )
        database.surgeryQueries.insertWithId(
            id = 81L,
            patientId = 1L,
            date = LocalDate(2026, 8, 1),
            type = null,
            description = null,
            outcome = null,
            surgeon = null,
            anesthesia = null,
            analgesia = null,
            complications = null,
            recoveryNotes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(5200L),
            updatedAt = Instant.fromEpochMilliseconds(5300L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.surgeryQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(LocalDate(2026, 5, 2), restored[0].date)
        assertEquals("Arthroscopy", restored[0].type)
        assertEquals("Chip removal from left front fetlock", restored[0].description)
        assertEquals("Excellent", restored[0].outcome)
        assertEquals("Dr. Costa", restored[0].surgeon)
        assertEquals("General - isoflurane", restored[0].anesthesia)
        assertEquals("Flunixin meglumine", restored[0].analgesia)
        assertEquals("Transient post-op swelling", restored[0].complications)
        assertEquals("Sutures out day 12, hand walking week 3", restored[0].recoveryNotes)
        assertEquals(false, restored[0].isActive)
        assertEquals(null, restored[1].type)
        assertEquals(true, restored[1].isActive)
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
