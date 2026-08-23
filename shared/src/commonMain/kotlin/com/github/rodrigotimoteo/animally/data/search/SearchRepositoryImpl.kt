package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.data.search.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Repository implementation managing the FTS5 global search index.
 *
 * Writes are applied inside a transaction so the metadata table
 * ([SearchFtsIndex]) and the FTS index ([SearchFts]) stay consistent.
 * The FTS rowid is kept aligned with the [SearchFtsIndex] id.
 */
@Single(binds = [ISearchRepository::class])
class SearchRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
    @Provided private val ownerQueries: OwnerQueries,
) : ISearchRepository {
    private val searchQueries: SearchFtsQueries = database.searchFtsQueries

    override fun indexRecord(
        recordType: String,
        patientId: Long,
        recordId: Long,
        date: LocalDate?,
        searchableText: String,
    ) {
        database.transaction {
            removeIndexRow(recordType, recordId)
            searchQueries.insertIndex(recordType, patientId, recordId, date, searchableText).value
            searchQueries.insertFts(searchableText).value
        }
    }

    override fun deleteRecord(
        recordType: String,
        recordId: Long,
    ) {
        database.transaction {
            removeIndexRow(recordType, recordId)
        }
    }

    override fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
        // FTS5 MATCH is token-exact; trailing each plain token with '*' turns
        // the query into a prefix match so partial input ("thun") finds
        // "Thunder". Tokens that are already FTS-syntax (starred terms, boolean
        // operators) pass through untouched.
        val matchQuery = toPrefixMatchQuery(query)
        if (matchQuery.isBlank()) return emptyList()
        val recordHits =
            searchQueries
                .search(matchQuery, from, to)
                .executeAsList()
                .filter { recordTypes == null || it.recordType in recordTypes }
                .map { it.toDomain() }
        val ownerHits =
            if (recordTypes == null || ISearchRepository.TYPE_OWNER in recordTypes) {
                searchQueries.searchOwners(matchQuery).executeAsList().map { it.toDomain() }
            } else {
                emptyList()
            }
        return recordHits + ownerHits
    }

    override fun rebuild() {
        database.transaction {
            searchQueries.deleteAllFts().value
            searchQueries.reseed().value
        }
    }

    override fun reindexOwners() {
        ownerQueries
            .selectAll()
            .executeAsList()
            .forEach { owner ->
                val searchableText =
                    listOfNotNull(owner.name, owner.email, owner.phone, owner.address)
                        .joinToString(" ")
                indexRecord(
                    recordType = ISearchRepository.TYPE_OWNER,
                    patientId = 0L,
                    recordId = owner.id,
                    date = null,
                    searchableText = searchableText,
                )
            }
    }

    override fun reindexPatients() {
        database.patientQueries
            .selectAll()
            .executeAsList()
            .forEach { patient ->
                val searchableText =
                    listOfNotNull(
                        patient.name,
                        patient.species,
                        patient.breed,
                        patient.microchipId,
                        patient.ueln,
                        patient.registrationNumber,
                        patient.stableLocation,
                    ).joinToString(" ")
                indexRecord(
                    recordType = ISearchRepository.TYPE_PATIENT,
                    patientId = patient.id,
                    recordId = patient.id,
                    date = null,
                    searchableText = searchableText,
                )
            }
    }

    /** Appends prefix stars to plain tokens, preserving explicit FTS syntax.
     * Non-alphanumeric characters (hyphens, punctuation) split tokens — FTS5
     * tokenizes content the same way, so "UITest-DF4D25" becomes
     * "uitest* df4d25*", matching how the text was indexed. */
    private fun toPrefixMatchQuery(query: String): String =
        query
            .split(Regex("\\s+"))
            .flatMap { token ->
                val isSyntax = token.endsWith("*") || token.uppercase() in setOf("AND", "OR", "NOT")
                if (isSyntax) {
                    listOf(token)
                } else {
                    token
                        .split(Regex("[^\\p{L}\\p{N}]+"))
                        .filter { it.isNotBlank() }
                        .map { "$it*" }
                }
            }.filter { it.isNotBlank() }
            .joinToString(" ")

    private fun removeIndexRow(
        recordType: String,
        recordId: Long,
    ) {
        val existing = searchQueries.selectIndexRow(recordType, recordId).executeAsOneOrNull()
        searchQueries.deleteIndex(recordType, recordId).value
        if (existing != null) {
            searchQueries.deleteFts(existing.id).value
        }
    }
}
