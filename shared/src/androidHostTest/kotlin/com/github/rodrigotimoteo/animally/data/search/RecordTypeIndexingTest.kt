package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Host-JVM regression tests for record-type indexing across the expanded
 * search surface: batch-number lookup and delete-path index cleanup.
 */
class RecordTypeIndexingTest {
    private val database = createTestDatabase()
    private val patientRepo = PatientRepositoryImpl(database)
    private val repo = SearchRepositoryImpl(database, database.ownerQueries)

    private fun seedPatient(name: String = "Thunder"): Long {
        val now = Clock.System.now()
        return patientRepo.insertPatient(
            Patient(id = 0, name = name, species = "Equine", createdAt = now, updatedAt = now),
        )
    }

    @Test
    fun givenIndexedVaccinationWhenSearchedByBatchNumberThenFound() {
        val patientId = seedPatient()
        repo.indexRecord(
            recordType = RecordType.Vaccination.wireName,
            patientId = patientId,
            recordId = 7L,
            date = null,
            searchableText = "Tetanus B12345 Dr. House",
        )

        val results = repo.search("B12345", null, null, null)

        assertEquals(1, results.size, "batch number must be findable via prefix search")
        assertEquals(RecordType.Vaccination.wireName, results.single().recordType)
        assertEquals(patientId, results.single().patientId)
    }

    @Test
    fun givenIndexedFarrierVisitWhenDeletedThenSearchNoLongerFindsIt() {
        val patientId = seedPatient()
        repo.indexRecord(
            recordType = RecordType.FarrierVisit.wireName,
            patientId = patientId,
            recordId = 9L,
            date = null,
            searchableText = "Full shoeing John Smith",
        )
        assertEquals(1, repo.search("shoeing", null, null, null).size)

        repo.deleteRecord(RecordType.FarrierVisit.wireName, 9L)

        assertTrue(repo.search("shoeing", null, null, null).isEmpty(), "deleted record must not stay searchable")
    }

    @Test
    fun givenReindexRecordsWhenRowsExistThenAllTypesBackfilled() {
        val patientId = seedPatient()
        // Seed the metadata table directly for two types, wipe FTS content, heal.
        repo.indexRecord(RecordType.LabResult.wireName, patientId, 3L, null, "CBC elevated")
        repo.indexRecord(RecordType.Weight.wireName, patientId, 4L, null, "512.5")
        database.searchFtsQueries.deleteAllFts().value

        repo.reindexRecords()

        // reindexRecords only backfills record tables; FTS realignment happens in rebuild().
        repo.rebuild()
        val labHits = repo.search("elevated", null, null, listOf(RecordType.LabResult.wireName))
        val weightHits = repo.search("512", null, null, listOf(RecordType.Weight.wireName))
        assertEquals(1, labHits.size)
        assertEquals(1, weightHits.size)
    }

    @Test
    fun givenRecordTypeFilterWhenSearchedThenOnlyMatchingTypesReturned() {
        val patientId = seedPatient()
        repo.indexRecord(ISearchRepository.TYPE_CONSULTATION, patientId, 1L, null, "colic exam")
        repo.indexRecord(RecordType.Surgery.wireName, patientId, 2L, null, "colic surgery")

        val results = repo.search("colic", null, null, listOf(RecordType.Surgery.wireName))

        assertEquals(1, results.size)
        assertEquals(RecordType.Surgery.wireName, results.single().recordType)
    }
}
