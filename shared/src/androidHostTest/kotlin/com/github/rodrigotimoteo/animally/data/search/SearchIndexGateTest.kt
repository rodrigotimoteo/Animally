package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

/**
 * Host-JVM tests for the startup healing gate: full reindexing must run only
 * when the stored index version differs or the index is empty.
 */
class SearchIndexGateTest {
    private val database = createTestDatabase()
    private val patientRepo = PatientRepositoryImpl(database)
    private val repo = SearchRepositoryImpl(database, database.ownerQueries)
    private val stateQueries = database.searchIndexStateQueries

    private fun seedPatient(name: String): Long {
        val now = Clock.System.now()
        return patientRepo.insertPatient(
            Patient(id = 0, name = name, species = "Equine", createdAt = now, updatedAt = now),
        )
    }

    @Test
    fun givenFreshDatabaseWhenReindexIfNeededThenIndexesAndStoresVersion() {
        val patientId = seedPatient("Thunder")
        // Consultation row exists but was never indexed (pre-healing data).
        database.consultationQueries.insert(
            patientId = patientId,
            date = LocalDate(2026, 1, 10),
            subjective = "colic signs",
            objective = "tension",
            assessment = "tendon injury",
            plan = "rest",
            vetName = "Dr. House",
            nextVisitDate = null,
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

        repo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)

        assertEquals(1, repo.search("thun", null, null, null).size, "patient must be searchable after healing")
        assertEquals(1, repo.search("tendon", null, null, null).size, "consultation text must be searchable after healing")
        assertEquals(
            ISearchRepository.SEARCH_INDEX_VERSION,
            stateQueries.selectState("search_index_version").executeAsOneOrNull(),
            "healed version must be persisted",
        )
    }

    @Test
    fun givenCurrentVersionWhenReindexIfNeededThenFastPathDoesNotDuplicate() {
        val patientId = seedPatient("Thunder")

        repo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)
        val rowsAfterFirst = database.searchFtsQueries.countIndexRows().executeAsOne()

        repo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)

        assertEquals(
            rowsAfterFirst,
            database.searchFtsQueries.countIndexRows().executeAsOne(),
            "fast path must not touch the index",
        )
    }

    @Test
    fun givenOldVersionWhenReindexIfNeededThenHealsAgain() {
        val patientId = seedPatient("Thunder")
        repo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)

        // Simulate drift: stale stored version and a clobbered index row.
        stateQueries.upsertState("search_index_version", "4")
        database.searchFtsQueries.deleteAllFts().value

        repo.reindexIfNeeded(ISearchRepository.SEARCH_INDEX_VERSION)

        assertEquals(1, repo.search("thun", null, null, null).size, "wiped FTS content must be re-seeded")
        assertEquals(
            ISearchRepository.SEARCH_INDEX_VERSION,
            stateQueries.selectState("search_index_version").executeAsOneOrNull(),
        )
    }

    @Test
    fun givenNeverHealedWhenReadVersionThenNullStored() {
        assertNull(stateQueries.selectState("search_index_version").executeAsOneOrNull())
    }
}
