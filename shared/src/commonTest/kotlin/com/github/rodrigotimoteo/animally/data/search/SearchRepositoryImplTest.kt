package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.SearchableText
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SearchRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: SearchRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = SearchRepositoryImpl(database, database.ownerQueries)
    }

    private fun insertPatient(name: String): Long =
        database.patientQueries
            .insert(
                name = name,
                species = "Equine",
                breed = "Hanoverian",
                dateOfBirth = LocalDate(2020, 5, 1),
                gender = "Mare",
                microchipId = "900123",
                ueln = null,
                registrationNumber = null,
                stableLocation = null,
                photoUri = null,
                notes = null,
                ownerId = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
                cogginsTestDate = null,
                cogginsResult = null,
                cogginsExpiryDate = null,
            ).value

    @Test
    fun `when patient indexed then search finds it`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Midnight Hanoverian 900123")

        val results = sut.search("midni*", null, null, null)

        assertEquals(1, results.size)
        assertEquals("Midnight", results.single().patientName)
        assertEquals(ISearchRepository.TYPE_PATIENT, results.single().recordType)
    }

    @Test
    fun `when patients are re-indexed then identity fields are searchable`() {
        val patientId = insertPatient("Midnight")

        sut.reindexPatients()

        val birthResults = sut.search("date of birth", null, null, null)
        assertEquals(1, birthResults.size)
        assertEquals(patientId, birthResults.single().patientId)
        assertTrue(birthResults.single().snippet.contains("2020-05-01"))

        val genderResults = sut.search("gender mare", null, null, null)
        assertEquals(listOf(patientId), genderResults.map { it.patientId })
    }

    @Test
    fun `when searching by medication name then finds indexed medication`() {
        val patientId = insertPatient("Midnight")
        val medication =
            Medication(
                id = 1L,
                patientId = patientId,
                name = "Flunixin",
                dosage = "500mg",
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
            )
        sut.indexRecord(ISearchRepository.TYPE_MEDICATION, patientId, medication.id, null, SearchableText.medication(medication))

        val results = sut.search("fluni*", null, null, null)

        assertEquals(1, results.size)
        assertEquals(ISearchRepository.TYPE_MEDICATION, results.single().recordType)
        assertTrue(results.single().snippet.contains("Flunixin 500mg"))

        val naturalQuestionResults = sut.search("prescription", null, null, null)
        assertEquals(listOf(medication.id), naturalQuestionResults.map { it.recordId })
    }

    @Test
    fun `when searching ultrasound snippets then findings beyond the query token remain visible`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(
            recordType = RecordType.Ultrasound.wireName,
            patientId = patientId,
            recordId = 1L,
            date = LocalDate(2026, 8, 20),
            searchableText =
                "left ovary active uterine echotexture corpus luteum present no dominant follicle " +
                    "ultrasound ecography ultrasonography findings examination viable singleton with heartbeat observed",
        )

        val results = sut.searchSnippets("ultrasound", null, null, null)

        assertEquals(1, results.size)
        assertTrue(results.single().snippet.contains("heartbeat"), results.single().snippet)
    }

    @Test
    fun `when searching snippets then returns the matching snippet`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(
            recordType = ISearchRepository.TYPE_PATIENT,
            patientId = patientId,
            recordId = patientId,
            date = null,
            searchableText = "Midnight Hanoverian horse",
        )

        val results = sut.searchSnippets("horse", null, null, null)

        assertEquals(1, results.size)
        assertTrue(results.single().snippet.contains("horse", ignoreCase = true))
    }

    @Test
    fun `when record deleted then search no longer finds it`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Midnight")

        sut.deleteRecord(ISearchRepository.TYPE_PATIENT, patientId)

        assertEquals(emptyList(), sut.search("midni*", null, null, null))
    }

    @Test
    fun `when record type filter applied then only matching types returned`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Midnight Hanoverian")
        sut.indexRecord(ISearchRepository.TYPE_MEDICATION, patientId, 1L, null, "Flunixin 500mg")

        val results = sut.search("midni* OR fluni*", null, null, listOf(ISearchRepository.TYPE_MEDICATION))

        assertEquals(1, results.size)
        assertEquals(ISearchRepository.TYPE_MEDICATION, results.single().recordType)
    }

    @Test
    fun `when date range applied then only records in range returned`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_CONSULTATION, patientId, 1L, LocalDate(2024, 3, 1), "Lameness exam")
        sut.indexRecord(ISearchRepository.TYPE_CONSULTATION, patientId, 2L, LocalDate(2024, 6, 1), "Lameness follow up")

        val results =
            sut.search(
                query = "lame*",
                from = LocalDate(2024, 4, 1),
                to = LocalDate(2024, 12, 31),
                recordTypes = null,
            )

        assertEquals(1, results.size)
        assertEquals(2L, results.single().recordId)
    }

    @Test
    fun `when FTS index desynced then rebuild re-seeds it`() {
        val patientId = insertPatient("Midnight")
        database.searchFtsQueries
            .insertIndex(
                recordType = ISearchRepository.TYPE_PATIENT,
                patientId = patientId,
                recordId = patientId,
                date = null,
                searchableText = "Midnight Hanoverian",
            ).value

        assertEquals(emptyList(), sut.search("midni*", null, null, null))

        sut.rebuild()

        val results = sut.search("midni*", null, null, null)
        assertEquals(1, results.size)
        assertEquals(patientId, results.single().patientId)
    }

    @Test
    fun `when re-indexing same record then old entry replaced`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Daylight")

        val results = sut.search("dayli*", null, null, null)

        assertEquals(1, results.size)
        assertEquals("Daylight", results.single().snippet)
        assertTrue(sut.search("midni*", null, null, null).isEmpty())
    }

    @Test
    fun `when healing index then stale records are removed`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_PATIENT, patientId, patientId, null, "Midnight")
        sut.indexRecord(ISearchRepository.TYPE_MEDICATION, patientId, 999L, null, "StaleDrug")

        sut.reindexIfNeeded("test-version")

        assertTrue(sut.search("stale*", null, null, null).isEmpty())
        assertEquals(1, sut.search("midni*", null, null, null).size)
    }
}
