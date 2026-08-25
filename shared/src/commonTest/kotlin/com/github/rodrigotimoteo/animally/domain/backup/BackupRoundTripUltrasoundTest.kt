package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for reproductive ultrasounds (incl.
 * migration-7 ovary/uterus columns and follicle child rows) and repro meds.
 */
class BackupRoundTripUltrasoundTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `ultrasound round-trips ovary uterus columns with follicle child rows`() {
        seedPatient()
        database.ultrasoundQueries.insertWithId(
            id = 11L,
            patientId = 1L,
            date = LocalDate(2026, 7, 1),
            ovaryStatus = "Active",
            uterineStatus = "Edematous",
            follicleSizeMm = 35.5,
            leftOvaryStatus = "Large follicle",
            rightOvaryStatus = "Inactive",
            leftFollicleSizeMm = 38.25,
            rightFollicleSizeMm = 21.75,
            uterineEdema = "Grade 2",
            uterineLiquid = true,
            uterineLiquidDescription = "Small amount of free fluid",
            uterusDescription = "Normal tone",
            findings = "Pre-ovulatory follicle",
            imageUris = null,
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1000L),
            updatedAt = Instant.fromEpochMilliseconds(2000L),
        )
        database.follicleQueries.insertWithId(
            id = 21L,
            ultrasoundId = 11L,
            side = "LEFT",
            sizeMm = 38.25,
            description = "Dominant",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1100L),
            updatedAt = Instant.fromEpochMilliseconds(2100L),
        )
        database.follicleQueries.insertWithId(
            id = 22L,
            ultrasoundId = 11L,
            side = "RIGHT",
            sizeMm = 21.75,
            description = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(1200L),
            updatedAt = Instant.fromEpochMilliseconds(2200L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restoredUltrasound =
            database.ultrasoundQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(11L, restoredUltrasound.id)
        assertEquals(LocalDate(2026, 7, 1), restoredUltrasound.date)
        assertEquals("Active", restoredUltrasound.ovaryStatus)
        assertEquals("Edematous", restoredUltrasound.uterineStatus)
        assertEquals(35.5, restoredUltrasound.follicleSizeMm)
        assertEquals("Large follicle", restoredUltrasound.leftOvaryStatus)
        assertEquals("Inactive", restoredUltrasound.rightOvaryStatus)
        assertEquals(38.25, restoredUltrasound.leftFollicleSizeMm)
        assertEquals(21.75, restoredUltrasound.rightFollicleSizeMm)
        assertEquals("Grade 2", restoredUltrasound.uterineEdema)
        assertEquals(true, restoredUltrasound.uterineLiquid)
        assertEquals("Small amount of free fluid", restoredUltrasound.uterineLiquidDescription)
        assertEquals("Normal tone", restoredUltrasound.uterusDescription)
        assertEquals("Pre-ovulatory follicle", restoredUltrasound.findings)
        assertEquals(null, restoredUltrasound.imageUris)
        assertEquals("Dr. Silva", restoredUltrasound.vetName)
        assertEquals(true, restoredUltrasound.isActive)

        val restoredFollicles =
            database.follicleQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restoredFollicles.size)
        assertEquals(21L, restoredFollicles[0].id)
        assertEquals(11L, restoredFollicles[0].ultrasoundId)
        assertEquals("LEFT", restoredFollicles[0].side)
        assertEquals(38.25, restoredFollicles[0].sizeMm)
        assertEquals("Dominant", restoredFollicles[0].description)
        assertEquals(true, restoredFollicles[0].isActive)
        assertEquals(22L, restoredFollicles[1].id)
        assertEquals("RIGHT", restoredFollicles[1].side)
        assertEquals(21.75, restoredFollicles[1].sizeMm)
        assertEquals(null, restoredFollicles[1].description)
        assertEquals(false, restoredFollicles[1].isActive)
    }

    @Test
    fun `repro medication round-trips dosage purpose and administration date`() {
        seedPatient()
        database.reproMedicationQueries.insertWithId(
            id = 71L,
            patientId = 1L,
            medication = "Deslorelin",
            dateAdministered = LocalDate(2026, 7, 3),
            dosage = "1.8 mg",
            purpose = "Induce ovulation",
            vetName = "Dr. Silva",
            notes = "IM injection",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(7000L),
            updatedAt = Instant.fromEpochMilliseconds(7100L),
        )
        database.reproMedicationQueries.insertWithId(
            id = 72L,
            patientId = 1L,
            medication = "hCG",
            dateAdministered = LocalDate(2026, 6, 20),
            dosage = null,
            purpose = null,
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(7200L),
            updatedAt = Instant.fromEpochMilliseconds(7300L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.reproMedicationQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals("Deslorelin", restored[0].medication)
        assertEquals(LocalDate(2026, 7, 3), restored[0].dateAdministered)
        assertEquals("1.8 mg", restored[0].dosage)
        assertEquals("Induce ovulation", restored[0].purpose)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals("IM injection", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].dosage)
        assertEquals(null, restored[1].purpose)
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
