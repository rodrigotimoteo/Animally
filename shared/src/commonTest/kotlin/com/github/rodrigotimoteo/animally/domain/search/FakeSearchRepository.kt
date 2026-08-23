package com.github.rodrigotimoteo.animally.domain.search

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate

/**
 * No-op fake of [ISearchRepository] for tests that do not exercise the search index.
 * Open so individual tests may override single methods.
 */
open class FakeSearchRepository : ISearchRepository {
    override fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> = emptyList()

    override fun indexRecord(
        recordType: String,
        patientId: Long,
        recordId: Long,
        date: LocalDate?,
        searchableText: String,
    ) = Unit

    override fun deleteRecord(
        recordType: String,
        recordId: Long,
    ) = Unit

    override fun rebuild() = Unit

    override fun reindexOwners() = Unit

    override fun reindexPatients() = Unit
}
