package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class RestoreBackupUseCaseTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `restore repopulates wiped database preserving ids flags and timestamps`() {
        seedDatabase()

        val json = exportJson()
        database.deleteAllBackupRows()
        assertTrue(
            database.patientQueries
                .selectAllRows()
                .executeAsList()
                .isEmpty(),
        )

        RestoreBackupUseCase(database).invoke(json)

        val restoredPatient =
            database.patientQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(1L, restoredPatient.id)
        assertEquals("Charlie", restoredPatient.name)
        assertEquals(LocalDate(2018, 3, 1), restoredPatient.dateOfBirth)
        assertEquals(Instant.fromEpochMilliseconds(1234L), restoredPatient.createdAt)

        val restoredConsultation =
            database.consultationQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(7L, restoredConsultation.id)
        assertEquals(1L, restoredConsultation.patientId)
        assertEquals("Suspected tendonitis", restoredConsultation.assessment)
        assertEquals(false, restoredConsultation.isActive)
        assertEquals(Instant.fromEpochMilliseconds(4321L), restoredConsultation.createdAt)

        val restoredWeight =
            database.weightQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(3L, restoredWeight.id)
        assertEquals(1L, restoredWeight.patientId)
        assertEquals(520.0, restoredWeight.weightKg)
    }

    @Test
    fun `restore rejects payload with unsupported schema version`() {
        seedDatabase()
        val json = BackupSerializer.encode(BackupSerializer.decode(exportJson()).copy(schemaVersion = 99))

        assertFailsWith<IllegalStateException> {
            RestoreBackupUseCase(database).invoke(json)
        }
    }

    @Test
    fun `restore round-trips migration-7 ultrasound fields and child rows`() {
        seedPatientOnly()
        database.ultrasoundQueries.insertWithId(
            id = 11L,
            patientId = 1L,
            date = LocalDate(2026, 7, 1),
            ovaryStatus = "Active",
            uterineStatus = "Edematous",
            follicleSizeMm = 35.5,
            leftOvaryStatus = "Large follicle",
            rightOvaryStatus = "Inactive",
            leftFollicleSizeMm = 38.0,
            rightFollicleSizeMm = 22.5,
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
            sizeMm = 38.0,
            description = "Dominant",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1100L),
            updatedAt = Instant.fromEpochMilliseconds(2100L),
        )
        database.follicleQueries.insertWithId(
            id = 22L,
            ultrasoundId = 11L,
            side = "RIGHT",
            sizeMm = 22.5,
            description = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(1200L),
            updatedAt = Instant.fromEpochMilliseconds(2200L),
        )
        database.reproductionQueries.insertWithId(
            id = 31L,
            patientId = 1L,
            eventType = "Breeding",
            date = LocalDate(2026, 7, 2),
            details = null,
            vetName = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1300L),
            updatedAt = Instant.fromEpochMilliseconds(2300L),
            initialExamFindings = "Good uterine tone",
            stallionName = "Cassiano",
            breedingType = "Natural cover",
        )
        database.embryoTransferQueries.insertWithId(
            id = 41L,
            patientId = 1L,
            date = LocalDate(2026, 7, 10),
            embryoCount = 2,
            recipientMares = "Mare A, Mare B",
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1400L),
            updatedAt = Instant.fromEpochMilliseconds(2400L),
        )
        database.icsiQueries.insertWithId(
            id = 51L,
            patientId = 1L,
            date = LocalDate(2026, 7, 12),
            folliclesRecovered = 5,
            vetName = "Dr. Silva",
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1500L),
            updatedAt = Instant.fromEpochMilliseconds(2500L),
        )

        val json = exportJson()
        database.deleteAllBackupRows()
        RestoreBackupUseCase(database).invoke(json)

        val restoredUltrasound =
            database.ultrasoundQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(11L, restoredUltrasound.id)
        assertEquals("Large follicle", restoredUltrasound.leftOvaryStatus)
        assertEquals("Inactive", restoredUltrasound.rightOvaryStatus)
        assertEquals(38.0, restoredUltrasound.leftFollicleSizeMm)
        assertEquals(22.5, restoredUltrasound.rightFollicleSizeMm)
        assertEquals("Grade 2", restoredUltrasound.uterineEdema)
        assertEquals(true, restoredUltrasound.uterineLiquid)
        assertEquals("Small amount of free fluid", restoredUltrasound.uterineLiquidDescription)
        assertEquals("Normal tone", restoredUltrasound.uterusDescription)
        assertEquals("Pre-ovulatory follicle", restoredUltrasound.findings)
        assertEquals(Instant.fromEpochMilliseconds(1000L), restoredUltrasound.createdAt)

        val restoredFollicles =
            database.follicleQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restoredFollicles.size)
        assertEquals(21L, restoredFollicles[0].id)
        assertEquals(11L, restoredFollicles[0].ultrasoundId)
        assertEquals("LEFT", restoredFollicles[0].side)
        assertEquals(38.0, restoredFollicles[0].sizeMm)
        assertEquals("Dominant", restoredFollicles[0].description)
        assertEquals(true, restoredFollicles[0].isActive)
        assertEquals(22L, restoredFollicles[1].id)
        assertEquals("RIGHT", restoredFollicles[1].side)
        assertEquals(false, restoredFollicles[1].isActive)

        val restoredReproduction =
            database.reproductionQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals("Good uterine tone", restoredReproduction.initialExamFindings)
        assertEquals("Cassiano", restoredReproduction.stallionName)
        assertEquals("Natural cover", restoredReproduction.breedingType)

        val restoredEmbryoTransfer =
            database.embryoTransferQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(41L, restoredEmbryoTransfer.id)
        assertEquals(2L, restoredEmbryoTransfer.embryoCount)
        assertEquals("Mare A, Mare B", restoredEmbryoTransfer.recipientMares)

        val restoredIcsi =
            database.icsiQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(51L, restoredIcsi.id)
        assertEquals(5L, restoredIcsi.folliclesRecovered)
    }

    @Test
    fun `restore accepts legacy payload without migration-7 keys`() {
        seedPatientOnly()
        val legacyJson =
            """
            {
              "schemaVersion": 1,
              "exportedAt": "2026-01-01T00:00:00Z",
              "patients": [],
              "owners": [],
              "anamnese": [],
              "consultations": [],
              "vaccinations": [],
              "weights": [],
              "dewormings": [],
              "dentistry": [],
              "lameness": [],
              "surgeries": [],
              "medications": [],
              "labResults": [],
              "imaging": [],
              "farrierVisits": [],
              "reproductionEvents": [
                {
                  "id": 31,
                  "patientId": 1,
                  "eventType": "Heat",
                  "date": "2026-06-01",
                  "details": null,
                  "vetName": null,
                  "notes": null,
                  "isActive": true,
                  "createdAt": 1000,
                  "updatedAt": 2000
                }
              ],
              "ultrasounds": [
                {
                  "id": 11,
                  "patientId": 1,
                  "date": "2026-07-01",
                  "ovaryStatus": "Active",
                  "uterineStatus": "Normal",
                  "follicleSizeMm": 35.5,
                  "findings": null,
                  "imageUris": null,
                  "vetName": null,
                  "notes": null,
                  "isActive": true,
                  "createdAt": 1000,
                  "updatedAt": 2000
                }
              ],
              "gestations": [],
              "reproMedications": [],
              "substances": []
            }
            """.trimIndent()

        RestoreBackupUseCase(database).invoke(legacyJson)

        val restoredUltrasound =
            database.ultrasoundQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(11L, restoredUltrasound.id)
        assertEquals(null, restoredUltrasound.leftOvaryStatus)
        assertEquals(null, restoredUltrasound.rightOvaryStatus)
        assertEquals(null, restoredUltrasound.leftFollicleSizeMm)
        assertEquals(null, restoredUltrasound.rightFollicleSizeMm)
        assertEquals(null, restoredUltrasound.uterineEdema)
        assertEquals(null, restoredUltrasound.uterineLiquid)
        assertEquals(null, restoredUltrasound.uterineLiquidDescription)
        assertEquals(null, restoredUltrasound.uterusDescription)

        val restoredReproduction =
            database.reproductionQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(null, restoredReproduction.initialExamFindings)
        assertEquals(null, restoredReproduction.stallionName)
        assertEquals(null, restoredReproduction.breedingType)

        assertTrue(
            database.follicleQueries
                .selectAllRows()
                .executeAsList()
                .isEmpty(),
        )
        assertTrue(
            database.embryoTransferQueries
                .selectAllRows()
                .executeAsList()
                .isEmpty(),
        )
        assertTrue(
            database.icsiQueries
                .selectAllRows()
                .executeAsList()
                .isEmpty(),
        )
    }

    private fun exportJson(): String {
        var content: String? = null
        ExportBackupUseCase(
            database = database,
            writeFile = { fileName, json ->
                content = json
                "backups/$fileName"
            },
            copyDatabase = { "backups/animally.db" },
        ).invoke()
        return requireNotNull(content)
    }

    private fun seedPatientOnly() {
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
            createdAt = Instant.fromEpochMilliseconds(1234L),
            updatedAt = Instant.fromEpochMilliseconds(1234L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
    }

    private fun seedDatabase() {
        seedPatientOnly()
        database.consultationQueries.insertWithId(
            id = 7L,
            patientId = 1L,
            date = LocalDate(2026, 6, 15),
            subjective = "Mild lameness",
            objective = null,
            assessment = "Suspected tendonitis",
            plan = null,
            vetName = null,
            nextVisitDate = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(4321L),
            updatedAt = Instant.fromEpochMilliseconds(4321L),
        )
        database.weightQueries.insertWithId(
            id = 3L,
            patientId = 1L,
            weightKg = 520.0,
            date = LocalDate(2026, 5, 1),
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
    }
}
