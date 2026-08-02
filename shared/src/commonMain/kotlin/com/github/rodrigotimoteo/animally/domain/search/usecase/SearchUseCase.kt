package com.github.rodrigotimoteo.animally.domain.search.usecase

import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for searching across all records via the FTS5 index.
 *
 * The raw query is tokenized into a prefix-match FTS5 expression before being
 * passed to the repository.
 */
@Single
class SearchUseCase(
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Searches for records matching [query].
     *
     * @param query The raw user-entered query.
     * @param from Optional lower bound for the record date (inclusive).
     * @param to Optional upper bound for the record date (inclusive).
     * @param recordTypes Optional filter restricting the record types returned.
     * @return the matching search results, or an empty list for a blank query.
     */
    operator fun invoke(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
        val ftsQuery = tokenize(query) ?: return emptyList()
        return searchRepository.search(ftsQuery, from, to, recordTypes)
    }

    private fun tokenize(query: String): String? {
        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token -> "$token*" }
    }
}
