package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

/**
 * Isolates the FTS search query from any UI-layer concerns: indexes a patient
 * and an owner exactly as production does, then exercises the raw query paths.
 */
class SearchIsolationTest {
    private fun buildRepo(): Pair<PatientRepositoryImpl, SearchRepositoryImpl> {
        val database = createTestDatabase()
        return PatientRepositoryImpl(database) to
            SearchRepositoryImpl(database, database.ownerQueries)
    }

    private fun seedThunder(
        patientRepo: PatientRepositoryImpl,
        repo: SearchRepositoryImpl,
    ): Long {
        val now = Clock.System.now()
        val id =
            patientRepo.insertPatient(
                Patient(id = 0, name = "Thunder", species = "Equine", createdAt = now, updatedAt = now),
            )
        repo.indexRecord(
            recordType = ISearchRepository.TYPE_PATIENT,
            patientId = id,
            recordId = id,
            date = null,
            searchableText = "Thunder Equine",
        )
        return id
    }

    @Test
    fun givenIndexedPatientWhenExactCaseSearchThenPatientReturned() {
        val (patientRepo, repo) = buildRepo()
        val id = seedThunder(patientRepo, repo)

        val results = repo.search("Thunder", null, null, null)

        assertEquals(1, results.size, "exact search should hit the indexed patient")
        assertEquals(id, results.first().patientId)
    }

    @Test
    fun givenIndexedPatientWhenLowercasePartialSearchThenPatientReturned() {
        val (patientRepo, repo) = buildRepo()
        seedThunder(patientRepo, repo)

        val results = repo.search("thun", null, null, null)

        assertEquals(1, results.size, "prefix search 'thun' should hit 'Thunder'")
    }

    @Test
    fun givenInsertedPatientWhenIdRequestedThenRealGeneratedIdReturned() {
        val database = createTestDatabase()
        val patientRepo = PatientRepositoryImpl(database)
        val now = Clock.System.now()

        val first = patientRepo.insertPatient(Patient(id = 0, name = "A", createdAt = now, updatedAt = now))
        val second = patientRepo.insertPatient(Patient(id = 0, name = "B", createdAt = now, updatedAt = now))

        assertEquals(1L, first, "first was $first")
        assertEquals(2L, second, "second was $second")
    }

    @Test
    fun givenHyphenatedNameWhenSearchedByFullHyphenatedQueryThenPatientReturned() {
        val (patientRepo, repo) = buildRepo()
        val now = Clock.System.now()
        val id =
            patientRepo.insertPatient(
                Patient(id = 0, name = "UITest-DF4D25", createdAt = now, updatedAt = now),
            )
        repo.indexRecord(
            recordType = ISearchRepository.TYPE_PATIENT,
            patientId = id,
            recordId = id,
            date = null,
            searchableText = "UITest-DF4D25 Equine",
        )

        val results = repo.search("UITest-DF4D25", null, null, null)

        assertEquals(1, results.size, "hyphenated query should match hyphenated name")
    }
}
