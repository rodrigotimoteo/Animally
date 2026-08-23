package com.github.rodrigotimoteo.animally.domain.search

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate

/**
 * Contract for the application-managed global search index backed by FTS5.
 *
 * The index is kept in sync incrementally from the save/delete paths of the
 * Patient, Consultation and Medication repositories.
 */
interface ISearchRepository {
    /**
     * Searches the FTS index for records matching [query].
     *
     * @param query The tokenized FTS5 MATCH expression (prefix tokens joined with `AND`).
     * @param from Optional lower bound for the record date (inclusive).
     * @param to Optional upper bound for the record date (inclusive).
     * @param recordTypes Optional filter restricting the record types returned.
     * @return the matching search results.
     */
    fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult>

    /**
     * Indexes (or replaces) the searchable record identified by [recordType] and [recordId].
     */
    fun indexRecord(
        recordType: String,
        patientId: Long,
        recordId: Long,
        date: LocalDate?,
        searchableText: String,
    )

    /**
     * Removes the searchable record identified by [recordType] and [recordId] from the index.
     */
    fun deleteRecord(
        recordType: String,
        recordId: Long,
    )

    /**
     * Re-seeds the FTS index from the metadata table.
     */
    fun rebuild()

    /**
     * Re-indexes every active owner. Idempotent: existing entries are replaced.
     * Called at startup so owners created before owner indexing existed appear
     * in search results.
     */
    fun reindexOwners()

    /** Re-indexes every active patient; heals rows whose index entry was lost or clobbered. */
    fun reindexPatients()

    companion object {
        const val TYPE_PATIENT = "PATIENT"
        const val TYPE_CONSULTATION = "CONSULTATION"
        const val TYPE_MEDICATION = "MEDICATION"
        const val TYPE_OWNER = "OWNER"
    }
}
