package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Host-JVM tests for [SearchRepositoryImpl] query-syntax handling: FTS boolean
 * operators must pass through unstarred, and blank input must short-circuit to
 * empty results without reaching SQL.
 *
 * Prefix and hyphenated-token behavior is covered by SearchIsolationTest.
 */
class SearchQuerySyntaxTest {
    private val database = createTestDatabase()
    private val patientRepo = PatientRepositoryImpl(database)
    private val repo = SearchRepositoryImpl(database, database.ownerQueries)

    private fun seedPatient(
        name: String,
        searchableText: String,
    ): Long {
        val now = Clock.System.now()
        val id =
            patientRepo.insertPatient(
                Patient(id = 0, name = name, species = "Equine", createdAt = now, updatedAt = now),
            )
        repo.indexRecord(
            recordType = ISearchRepository.TYPE_PATIENT,
            patientId = id,
            recordId = id,
            date = null,
            searchableText = searchableText,
        )
        return id
    }

    @Test
    fun givenOrQueryWhenSearchedThenOperatorPassesThroughAndEitherSideMatches() {
        val thunderId = seedPatient("Thunder", "Thunder Equine")
        seedPatient("Lightning", "Lightning Equine")

        val results = repo.search("thunder OR lightning", null, null, null)

        assertEquals(2, results.size, "OR must act as a boolean operator, not a literal token")
        assertTrue(results.any { it.patientId == thunderId })
    }

    @Test
    fun givenAndQueryWhenSearchedThenOnlyDocumentsContainingBothTokensMatch() {
        seedPatient("Thunder", "Thunder Equine")
        seedPatient("Storm", "Storm Chaser")

        val results = repo.search("thunder AND equine", null, null, null)

        assertEquals(1, results.size, "AND must require both tokens in the same document")
        assertEquals("Thunder", results.single().patientName)
    }

    @Test
    fun givenEmptyQueryWhenSearchedThenEmptyListWithoutSqlError() {
        seedPatient("Thunder", "Thunder Equine")

        assertTrue(repo.search("", null, null, null).isEmpty())
    }

    @Test
    fun givenWhitespaceOnlyQueryWhenSearchedThenEmptyListWithoutSqlError() {
        seedPatient("Thunder", "Thunder Equine")

        assertTrue(repo.search("   \t ", null, null, null).isEmpty())
    }

    @Test
    fun givenPunctuationOnlyQueryWhenSearchedThenEmptyListWithoutSqlError() {
        seedPatient("Thunder", "Thunder Equine")

        // A lone hyphen splits into no alphanumeric tokens -> blank match query.
        assertTrue(repo.search("-", null, null, null).isEmpty())
    }
}
