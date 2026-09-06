package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.owner.OwnerQueries
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Facade over the FTS5 global search index.
 *
 * Query execution lives in [SearchQueryEngine], bulk reindexing in
 * [SearchIndexerRegistry]. Writes stay here so the metadata table
 * ([SearchFtsIndex]) and the FTS index ([SearchFts]) remain transactionally
 * consistent — the FTS rowid is kept aligned with the [SearchFtsIndex] id.
 */
@Single(binds = [ISearchRepository::class])
class SearchRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
    @Provided private val ownerQueries: OwnerQueries,
) : ISearchRepository {
    private val searchQueries: SearchFtsQueries = database.searchFtsQueries
    private val queryEngine = SearchQueryEngine(searchQueries)
    private val indexerRegistry = SearchIndexerRegistry(database, ownerQueries)

    /**
     * Suppresses save-time FTS writes while the bulk heal runs: only the
     * metadata table is written; [rebuild] performs the single FTS build
     * afterwards. Save-time single-record indexing keeps writing both tables.
     *
     * Confined to the main thread by convention: reindexIfNeeded is invoked
     * once at app start and every indexRecord caller is UI-driven on the same
     * thread. A future background writer would need real synchronization here.
     */
    private var suppressFtsWrites = false

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
            if (!suppressFtsWrites) {
                searchQueries.insertFts(searchableText).value
            }
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
    ): List<SearchResult> = queryEngine.search(query, from, to, recordTypes)

    override fun searchSnippets(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> = queryEngine.searchSnippets(query, from, to, recordTypes)

    override fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> = queryEngine.searchByDateRange(from, to)

    override fun rebuild() {
        database.transaction {
            searchQueries.deleteAllFts().value
            searchQueries.reseed().value
        }
    }

    override fun markIndexDirty() {
        database.searchIndexStateQueries.deleteAll()
    }

    fun reindexOwners() {
        indexerRegistry.reindexOwners(::indexRecord)
    }

    fun reindexPatients() {
        indexerRegistry.reindexPatients(::indexRecord)
    }

    fun reindexRecords() {
        indexerRegistry.reindexRecords(::indexRecord)
    }

    override fun reindexIfNeeded(indexVersion: String) {
        val storedVersion =
            database.searchIndexStateQueries
                .selectState(VERSION_KEY)
                .executeAsOneOrNull()
        val metadataCount = searchQueries.countIndexRows().executeAsOne()
        val ftsCount = searchQueries.countFtsRows().executeAsOne()
        val missingFtsRows = searchQueries.countIndexRowsMissingFromFts().executeAsOne()
        val orphanFtsRows = searchQueries.countFtsRowsMissingFromIndex().executeAsOne()
        val mismatchedFtsRows = searchQueries.countMismatchedFtsRows().executeAsOne()
        val projectionIsHealthy =
            metadataCount == ftsCount &&
                missingFtsRows == 0L &&
                orphanFtsRows == 0L &&
                mismatchedFtsRows == 0L
        if (storedVersion == indexVersion && projectionIsHealthy) {
            return
        }
        suppressFtsWrites = true
        try {
            database.transaction {
                searchQueries.deleteAllFts().value
                searchQueries.deleteAllIndex().value
            }
            reindexOwners()
            reindexPatients()
            reindexRecords()
        } finally {
            suppressFtsWrites = false
        }
        rebuild()
        database.searchIndexStateQueries.upsertState(VERSION_KEY, indexVersion)
    }

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

    private companion object {
        const val VERSION_KEY = "search_index_version"
    }
}
