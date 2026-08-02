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

    private fun seedDatabase() {
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
