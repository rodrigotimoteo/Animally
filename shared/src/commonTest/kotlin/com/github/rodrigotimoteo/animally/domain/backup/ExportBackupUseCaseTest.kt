package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportBackupUseCaseTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `export writes backup file containing seeded records`() {
        database.ownerQueries.insert(
            name = "Jane Doe",
            email = "jane@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        database.patientQueries.insert(
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
            ownerId = 1L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.consultationQueries.insert(
            patientId = 1L,
            date = LocalDate(2026, 6, 15),
            subjective = "Mild lameness",
            objective = null,
            assessment = "Suspected tendonitis",
            plan = null,
            vetName = null,
            nextVisitDate = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )

        var writtenFileName: String? = null
        var writtenContent: String? = null
        val useCase =
            ExportBackupUseCase(
                database = database,
                writeFile = { fileName, content ->
                    writtenFileName = fileName
                    writtenContent = content
                    "backups/$fileName"
                },
                copyDatabase = { "backups/animally.db" },
            )

        val result = useCase()

        val payload = BackupSerializer.decode(requireNotNull(writtenContent))
        assertEquals(1, payload.owners.size)
        assertEquals("Jane Doe", payload.owners.single().name)
        assertEquals(1, payload.patients.size)
        assertEquals("Charlie", payload.patients.single().name)
        assertEquals(1, payload.consultations.size)
        assertEquals("Suspected tendonitis", payload.consultations.single().assessment)
        assertEquals(requireNotNull(writtenFileName), result.fileName)
        assertTrue(result.fileName.startsWith("backup_"))
        assertTrue(result.fileName.endsWith(".json"))
        assertEquals("backups/${result.fileName}", result.backupPath)
        assertEquals("backups/animally.db", result.dbCopyPath)
    }

    @Test
    fun `export then restore round-trips seeded patient`() {
        database.ownerQueries.insert(
            name = "Jane Doe",
            email = null,
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        database.patientQueries.insert(
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
            ownerId = 1L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )

        var json: String? = null
        val useCase =
            ExportBackupUseCase(
                database = database,
                writeFile = { fileName, content ->
                    json = content
                    "backups/$fileName"
                },
                copyDatabase = { "backups/animally.db" },
            )
        useCase()

        assertTrue(requireNotNull(json).contains("Charlie"))
        database.deleteAllBackupRows()
        assertTrue(
            database.patientQueries
                .selectAllRows()
                .executeAsList()
                .isEmpty(),
        )

        RestoreBackupUseCase(database).invoke(requireNotNull(json))

        val restored =
            database.patientQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals("Charlie", restored.name)
        assertEquals("Hanoverian", restored.breed)
        assertEquals(1L, restored.ownerId)
    }

    @Test
    fun `export includes soft-deleted rows`() {
        database.patientQueries.insert(
            name = "Ghost",
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
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )

        var writtenContent: String? = null
        val useCase =
            ExportBackupUseCase(
                database = database,
                writeFile = { fileName, content ->
                    writtenContent = content
                    "backups/$fileName"
                },
                copyDatabase = { "backups/animally.db" },
            )

        useCase()

        val payload = BackupSerializer.decode(requireNotNull(writtenContent))
        assertEquals(1, payload.patients.size)
        assertEquals(false, payload.patients.single().isActive)
    }
}
