package com.github.rodrigotimoteo.animally.domain.settings

import app.cash.sqldelight.Query
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.settings.usecase.WipeAllDataUseCase
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Seeds rows across several tables plus both halves of the FTS index, wipes,
 * and asserts every table and the index are empty.
 */
class WipeAllDataUseCaseTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var searchRepository: SearchRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        searchRepository = SearchRepositoryImpl(database, database.ownerQueries)
    }

    @Test
    fun `wipe empties every seeded table and the search index`() {
        seedRows()
        seedSearchIndex()

        WipeAllDataUseCase(database, searchRepository).invoke()

        assertEmpty { database.ownerQueries.selectAll() }
        assertEmpty { database.patientQueries.selectAllRows() }
        assertEmpty { database.anamneseQueries.selectAllRows() }
        assertEmpty { database.consultationQueries.selectAllRows() }
        assertEmpty { database.vaccinationQueries.selectAll() }
        assertEmpty { database.weightQueries.selectAllRows() }
        assertEmpty { database.customReminderQueries.selectAllRows() }

        // Both halves of the FTS index: metadata table and the full-text table.
        assertEquals(0L, database.searchFtsQueries.countIndexRows().executeAsOne())
        assertTrue(searchRepository.search("Charlie", null, null, null).isEmpty())
    }

    /** Runs [query] and asserts it returned no rows. */
    private fun assertEmpty(query: () -> Query<*>) {
        assertTrue(query().executeAsList().isEmpty())
    }

    private fun seedRows() {
        database.ownerQueries.insertWithId(
            id = 1L,
            name = "Ana Souza",
            email = "ana@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(100L),
            updatedAt = Instant.fromEpochMilliseconds(100L),
        )
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
            ownerId = 1L,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1234L),
            updatedAt = Instant.fromEpochMilliseconds(1234L),
            cogginsTestDate = null,
            cogginsResult = null,
            cogginsExpiryDate = null,
        )
        database.anamneseQueries.insertWithId(
            id = 2L,
            patientId = 1L,
            generalHistory = "Chronic cough",
            chronicConditions = null,
            allergies = "Penicillin",
            createdAt = Instant.fromEpochMilliseconds(1500L),
            updatedAt = Instant.fromEpochMilliseconds(1500L),
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
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(4321L),
            updatedAt = Instant.fromEpochMilliseconds(4321L),
        )
        database.vaccinationQueries.insertWithId(
            id = 5L,
            patientId = 1L,
            vaccineName = "Tetanus",
            batchNumber = null,
            dateAdministered = LocalDate(2026, 5, 10),
            nextDueDate = LocalDate(2027, 5, 10),
            vetName = null,
            site = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(2000L),
            updatedAt = Instant.fromEpochMilliseconds(2000L),
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
        database.customReminderQueries.insertWithId(
            id = 9L,
            patientId = 1L,
            title = "Check shoe",
            dueDate = LocalDate(2026, 9, 1),
            linkedRecordType = null,
            linkedRecordId = null,
            notes = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(3000L),
            updatedAt = Instant.fromEpochMilliseconds(3000L),
        )
    }

    /** Writes one metadata row + its FTS twin directly, bypassing the repository. */
    private fun seedSearchIndex() {
        database.transaction {
            database.searchFtsQueries
                .insertIndex(ISearchRepository.TYPE_PATIENT, 1L, 1L, null, "Charlie Hanoverian")
                .value
            database.searchFtsQueries.insertFts("Charlie Hanoverian").value
        }
    }
}
