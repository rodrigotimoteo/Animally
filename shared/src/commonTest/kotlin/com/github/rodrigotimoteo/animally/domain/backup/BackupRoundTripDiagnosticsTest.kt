package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for clinical diagnostics:
 * lameness evaluations, lab results and imaging studies.
 */
class BackupRoundTripDiagnosticsTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `lameness round-trips all aaep grades limb locations and flexion results`() {
        seedPatient()
        val grades = listOf<Long>(1L, 2L, 3L, 4L, 5L)
        val limbs = listOf("LF", "RF", "LH", "RH", "Multi")
        grades.forEachIndexed { index, grade ->
            database.lamenessQueries.insertWithId(
                id = 50L + index,
                patientId = 1L,
                date = LocalDate(2026, 6, 15 + index),
                gradeAAEP = grade,
                limbLocation = limbs[index],
                flexionTest = if (index % 2 == 0) "Positive" else null,
                diagnosis = if (index == 4) "Severe bilateral lameness" else null,
                treatment = if (index == 4) "NSAIDs and stall rest" else null,
                vetName = "Dr. Silva",
                notes = if (index % 2 == 1) "Recheck in 2 weeks" else null,
                isActive = index != 4,
                createdAt = Instant.fromEpochMilliseconds(2000L + index * 10),
                updatedAt = Instant.fromEpochMilliseconds(2500L + index * 10),
            )
        }

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.lamenessQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(5, restored.size)
        grades.forEachIndexed { index, grade ->
            val row = restored[index]
            assertEquals(50L + index, row.id)
            assertEquals(grade, row.gradeAAEP)
            assertEquals(limbs[index], row.limbLocation)
            assertEquals(if (index % 2 == 0) "Positive" else null, row.flexionTest)
            assertEquals(if (index == 4) "Severe bilateral lameness" else null, row.diagnosis)
            assertEquals(if (index == 4) "NSAIDs and stall rest" else null, row.treatment)
            assertEquals("Dr. Silva", row.vetName)
            assertEquals(index != 4, row.isActive)
            assertEquals(Instant.fromEpochMilliseconds(2000L + index * 10), row.createdAt)
            assertEquals(Instant.fromEpochMilliseconds(2500L + index * 10), row.updatedAt)
        }
    }

    @Test
    fun `lab result round-trips test type results normal range and soft-delete`() {
        seedPatient()
        database.labResultQueries.insertWithId(
            id = 60L,
            patientId = 1L,
            testType = "Complete blood count",
            date = LocalDate(2026, 6, 16),
            results = "WBC 9.2 x10^3/uL, HCT 38%",
            normalRange = "WBC 5.9-11.4, HCT 32-48",
            vetName = "Dr. Silva",
            notes = "Within reference interval",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(3000L),
            updatedAt = Instant.fromEpochMilliseconds(3100L),
        )
        database.labResultQueries.insertWithId(
            id = 61L,
            patientId = 1L,
            testType = "Serum amyloid A",
            date = LocalDate(2026, 7, 1),
            results = null,
            normalRange = null,
            vetName = null,
            notes = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(3200L),
            updatedAt = Instant.fromEpochMilliseconds(3300L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.labResultQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals("Complete blood count", restored[0].testType)
        assertEquals(LocalDate(2026, 6, 16), restored[0].date)
        assertEquals("WBC 9.2 x10^3/uL, HCT 38%", restored[0].results)
        assertEquals("WBC 5.9-11.4, HCT 32-48", restored[0].normalRange)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals("Within reference interval", restored[0].notes)
        assertEquals(true, restored[0].isActive)
        assertEquals(null, restored[1].results)
        assertEquals(false, restored[1].isActive)
    }

    @Test
    fun `imaging round-trips type findings image uris and timestamps`() {
        seedPatient()
        database.imagingQueries.insertWithId(
            id = 70L,
            patientId = 1L,
            type = "Radiograph",
            date = LocalDate(2026, 6, 20),
            findings = "Remodeling of navicular bone margins",
            imageUris = "file:///img/rad1.jpg,file:///img/rad2.jpg",
            vetName = "Dr. Silva",
            notes = "Four views acquired",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4000L),
            updatedAt = Instant.fromEpochMilliseconds(4100L),
        )

        val json = exportAndWipe()
        restoreBackupUseCase(database).invoke(json)

        val restored =
            database.imagingQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(70L, restored.id)
        assertEquals(1L, restored.patientId)
        assertEquals("Radiograph", restored.type)
        assertEquals(LocalDate(2026, 6, 20), restored.date)
        assertEquals("Remodeling of navicular bone margins", restored.findings)
        assertEquals("file:///img/rad1.jpg,file:///img/rad2.jpg", restored.imageUris)
        assertEquals("Dr. Silva", restored.vetName)
        assertEquals("Four views acquired", restored.notes)
        assertEquals(true, restored.isActive)
        assertEquals(Instant.fromEpochMilliseconds(4000L), restored.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(4100L), restored.updatedAt)
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
