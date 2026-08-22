package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
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
    fun `when searching by medication name then finds indexed medication`() {
        val patientId = insertPatient("Midnight")
        sut.indexRecord(ISearchRepository.TYPE_MEDICATION, patientId, 1L, null, "Flunixin 500mg")

        val results = sut.search("fluni*", null, null, null)

        assertEquals(1, results.size)
        assertEquals(ISearchRepository.TYPE_MEDICATION, results.single().recordType)
        assertEquals("Flunixin 500mg", results.single().snippet)
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
}
