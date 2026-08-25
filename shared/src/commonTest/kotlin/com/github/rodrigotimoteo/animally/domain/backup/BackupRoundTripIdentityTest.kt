package com.github.rodrigotimoteo.animally.domain.backup

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Export -> wipe -> restore round-trips for the clinical identity family,
 * asserting every mapped field survives.
 */
class BackupRoundTripIdentityTest {
    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    @Test
    fun `owner round-trips contact fields`() {
        seedPatient()
        database.ownerQueries.insertWithId(
            id = 9L,
            name = "Jane Doe",
            email = "jane@example.com",
            phone = "+55 11 99999-0000",
            address = "Fazenda Santa Rita",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(100L),
            updatedAt = Instant.fromEpochMilliseconds(200L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.ownerQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(9L, restored.id)
        assertEquals("Jane Doe", restored.name)
        assertEquals("jane@example.com", restored.email)
        assertEquals("+55 11 99999-0000", restored.phone)
        assertEquals("Fazenda Santa Rita", restored.address)
        assertEquals(true, restored.isActive)
        assertEquals(Instant.fromEpochMilliseconds(100L), restored.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(200L), restored.updatedAt)
    }

    @Test
    fun `patient round-trips identifiers coggins fields and soft-delete`() {
        seedPatient()
        database.patientQueries.insertWithId(
            id = 2L,
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
            createdAt = Instant.fromEpochMilliseconds(350L),
            updatedAt = Instant.fromEpochMilliseconds(450L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val patients =
            database.patientQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, patients.size)

        val complete = patients[0]
        assertEquals(1L, complete.id)
        assertEquals("Charlie", complete.name)
        assertEquals("Equine", complete.species)
        assertEquals("Hanoverian", complete.breed)
        assertEquals(LocalDate(2018, 3, 1), complete.dateOfBirth)
        assertEquals("Mare", complete.gender)
        assertEquals("981020000000000", complete.microchipId)
        assertEquals("052000100000000", complete.ueln)
        assertEquals("DE409020010405", complete.registrationNumber)
        assertEquals("Box 12", complete.stableLocation)
        assertEquals("file:///photos/charlie.jpg", complete.photoUri)
        assertEquals("Chronic tendon issue", complete.notes)
        assertEquals(null, complete.ownerId)
        assertEquals(true, complete.isActive)
        assertEquals(LocalDate(2026, 1, 10), complete.cogginsTestDate)
        assertEquals("Negative", complete.cogginsResult)
        assertEquals(LocalDate(2026, 7, 10), complete.cogginsExpiryDate)
        assertEquals(Instant.fromEpochMilliseconds(300L), complete.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(400L), complete.updatedAt)

        val softDeleted = patients[1]
        assertEquals(2L, softDeleted.id)
        assertEquals(false, softDeleted.isActive)
        assertEquals(null, softDeleted.cogginsTestDate)
        assertEquals(null, softDeleted.dateOfBirth)
    }

    @Test
    fun `anamnese round-trips history chronic conditions and allergies`() {
        seedPatient()
        database.anamneseQueries.insertWithId(
            id = 10L,
            patientId = 1L,
            generalHistory = "Colic surgery 2023",
            chronicConditions = "Recurrent airway obstruction",
            allergies = "Penicillin",
            createdAt = Instant.fromEpochMilliseconds(500L),
            updatedAt = Instant.fromEpochMilliseconds(600L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.anamneseQueries
                .selectAllRows()
                .executeAsList()
                .single()
        assertEquals(10L, restored.id)
        assertEquals(1L, restored.patientId)
        assertEquals("Colic surgery 2023", restored.generalHistory)
        assertEquals("Recurrent airway obstruction", restored.chronicConditions)
        assertEquals("Penicillin", restored.allergies)
        assertEquals(Instant.fromEpochMilliseconds(500L), restored.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(600L), restored.updatedAt)
    }

    @Test
    fun `consultation round-trips full soap block next visit date and soft-delete`() {
        seedPatient()
        database.consultationQueries.insertWithId(
            id = 20L,
            patientId = 1L,
            date = LocalDate(2026, 6, 15),
            subjective = "Owner reports stiffness after work",
            objective = "Grade 2 LF lameness, heat in hoof",
            assessment = "Suspected mild navicular syndrome",
            plan = "Rest 30 days, recheck radiographs",
            vetName = "Dr. Silva",
            nextVisitDate = LocalDate(2026, 7, 15),
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(700L),
            updatedAt = Instant.fromEpochMilliseconds(800L),
        )
        database.consultationQueries.insertWithId(
            id = 21L,
            patientId = 1L,
            date = LocalDate(2026, 7, 20),
            subjective = null,
            objective = null,
            assessment = null,
            plan = null,
            vetName = null,
            nextVisitDate = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(810L),
            updatedAt = Instant.fromEpochMilliseconds(820L),
        )

        val json = exportAndWipe()
        RestoreBackupUseCase(database).invoke(json)

        val restored =
            database.consultationQueries
                .selectAllRows()
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(2, restored.size)
        assertEquals(LocalDate(2026, 6, 15), restored[0].date)
        assertEquals("Owner reports stiffness after work", restored[0].subjective)
        assertEquals("Grade 2 LF lameness, heat in hoof", restored[0].objective)
        assertEquals("Suspected mild navicular syndrome", restored[0].assessment)
        assertEquals("Rest 30 days, recheck radiographs", restored[0].plan)
        assertEquals("Dr. Silva", restored[0].vetName)
        assertEquals(LocalDate(2026, 7, 15), restored[0].nextVisitDate)
        assertEquals(false, restored[0].isActive)
        assertEquals(null, restored[1].subjective)
        assertEquals(null, restored[1].nextVisitDate)
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
            microchipId = "981020000000000",
            ueln = "052000100000000",
            registrationNumber = "DE409020010405",
            stableLocation = "Box 12",
            photoUri = "file:///photos/charlie.jpg",
            notes = "Chronic tendon issue",
            ownerId = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(300L),
            updatedAt = Instant.fromEpochMilliseconds(400L),
            cogginsTestDate = LocalDate(2026, 1, 10),
            cogginsResult = "Negative",
            cogginsExpiryDate = LocalDate(2026, 7, 10),
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
