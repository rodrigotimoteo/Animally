package com.github.rodrigotimoteo.animally.data.search

import kotlinx.datetime.LocalDate

/**
 * Writes a single searchable record into the FTS index.
 *
 * @param recordType wire name of the record type
 * @param patientId owning patient id (0 for owner rows)
 * @param recordId id of the source record
 * @param date optional date associated with the record
 * @param searchableText canonical searchable text for the record
 */
internal typealias SearchIndexWriter = (
    recordType: String,
    patientId: Long,
    recordId: Long,
    date: LocalDate?,
    searchableText: String,
) -> Unit

/**
 * Per-entity indexer that enumerates one record table and emits searchable rows.
 */
internal interface SearchIndexer {
    fun reindex(writer: SearchIndexWriter)
}
