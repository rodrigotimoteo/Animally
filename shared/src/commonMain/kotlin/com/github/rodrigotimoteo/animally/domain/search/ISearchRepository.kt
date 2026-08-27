package com.github.rodrigotimoteo.animally.domain.search

import com.github.rodrigotimoteo.animally.domain.common.RecordType
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
     * RAG-facing variant of [search]: identical matching, filtering and BM25
     * ordering, but each hit's [SearchResult.snippet] carries an FTS5 snippet
     * window (24 tokens around the first match) instead of the full indexed
     * text so long records cannot exhaust the assistant's context budget.
     * The global Search screen keeps using [search] with full text.
     */
    fun searchSnippets(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult>

    /**
     * Returns every active dated record in the inclusive window. This is used
     * for date-only assistant questions where FTS terms would be filler and a
     * relevance search could select an unrelated row.
     */
    fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> = emptyList()

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

    /**
     * Backfills the index for every clinical/preventive/reproductive record type
     * from its table; heals rows created before record-type indexing existed.
     * Idempotent: existing entries are replaced.
     */
    fun reindexRecords()

    /**
     * Runs the full healing pass ([reindexOwners], [reindexPatients],
     * [reindexRecords], [rebuild]) only when needed: the stored healed version
     * differs from [indexVersion] or the index is empty. Otherwise a no-op.
     *
     * @param indexVersion the version the caller expects the index to be healed for.
     */
    fun reindexIfNeeded(indexVersion: String)

    companion object {
        /** Wire names are sourced from [RecordType] so the two cannot drift. */
        val TYPE_PATIENT = RecordType.Patient.wireName
        val TYPE_CONSULTATION = RecordType.Consultation.wireName
        val TYPE_MEDICATION = RecordType.Medication.wireName
        val TYPE_OWNER = RecordType.Owner.wireName

        /**
         * Current search-index layout version. Bump whenever indexing logic,
         * indexed record types, or searchableText field selection changes so
         * every launch after the change performs one healing pass.
         *
         * v7: vaccination rows gained generic vaccination vocabulary and
         * embryo-transfer rows gained embryo-transfer vocabulary in their
         * indexed text (natural questions like "vaccination" / "embryo
         * transfer" previously zeroed out on raw field values alone).
         *
         * v8: lameness rows gained the "grade flexion" field labels, surgery
         * rows the "surgeon" label, and controlled-substance rows the
         * "witness" label, so dictation-adjacent field-label questions ("who
         * was the surgeon") hit the rows.
         *
         * v9: farrier-visit rows gained the "farrier visit trim shoeing
         * care" vocabulary (sparse UI-created visits with no hoof-care words
         * were unreachable by any natural farrier question), and query
         * sanitization gained the short-prefix guard (alphabetic tokens
         * under 3 chars match exactly instead of star-joining).
         *
         * v10: anamnese and custom-reminder rows are indexed, embryo/ICSI
         * vocabulary is shared with save-time indexing, and patient search
         * includes the complete identity fields plus notes.
         *
         * v11: medication rows are included in full healing, and healing starts
         * from an empty derived index so rows for records deleted since the
         * previous healing pass cannot remain searchable.
         *
         * v12: force one clean rebuild before date-scoped assistant activity
         * queries rely on the metadata table as their authoritative row set.
         *
         * v13: gestation rows include the recorded breeding date and breeding
         * vocabulary, and resolved Foaled rows no longer inherit active-pregnancy
         * search terms.
         */
        const val SEARCH_INDEX_VERSION = "13"
    }
}
