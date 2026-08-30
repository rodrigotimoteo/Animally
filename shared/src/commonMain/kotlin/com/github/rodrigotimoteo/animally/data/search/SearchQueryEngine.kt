package com.github.rodrigotimoteo.animally.data.search

import com.github.rodrigotimoteo.animally.data.search.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.datetime.LocalDate

/**
 * Executes FTS queries against [SearchFtsQueries] and sanitizes user input into safe MATCH expressions.
 *
 * Sanitization keeps prefix search for partial input while guarding short
 * alphabetic prefixes that would otherwise explode onto the whole corpus.
 */
internal class SearchQueryEngine(
    private val searchQueries: SearchFtsQueries,
) {
    fun search(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
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

    fun searchSnippets(
        query: String,
        from: LocalDate?,
        to: LocalDate?,
        recordTypes: List<String>?,
    ): List<SearchResult> {
        val matchQuery = toPrefixMatchQuery(query)
        if (matchQuery.isBlank()) return emptyList()
        val recordHits =
            searchQueries
                .searchSnippets(matchQuery, from, to)
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

    fun searchByDateRange(
        from: LocalDate,
        to: LocalDate,
    ): List<SearchResult> =
        searchQueries
            .searchByDateRange(from, to)
            .executeAsList()
            .map { it.toDomain() }

    /**
     * Builds a safe FTS5 MATCH query from raw user input.
     * Plain tokens are split on non-alphanumeric characters and each surviving
     * word gets a trailing prefix star. Short alphabetic prefixes match exactly
     * instead of star-joining. See [SearchRepositoryImpl] docs for full contract.
     */
    private fun toPrefixMatchQuery(query: String): String =
        queryPartRegex
            .findAll(query)
            .flatMap { match ->
                val quoted = match.groupValues[1]
                if (match.groupValues[2].isEmpty()) {
                    listOfNotNull(toQuotedPhrase(quoted))
                } else {
                    val token = match.groupValues[2]
                    if (token.uppercase() in BOOLEAN_OPERATORS) {
                        listOf(token)
                    } else {
                        token
                            .replace("*", "")
                            .split(Regex("[^\\p{L}\\p{N}]+"))
                            .filter { it.isNotBlank() }
                            .map(::starOrExact)
                    }
                }
            }.joinToString(" ")

    private fun keepsPrefixStar(word: String): Boolean = word.length >= MIN_PREFIX_STAR_CHARS || word.all(Char::isDigit)

    private fun starOrExact(word: String): String = if (keepsPrefixStar(word)) "$word*" else "\"$word\""

    private fun toQuotedPhrase(content: String): String? {
        val words =
            content
                .replace("*", "")
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val body = "\"${words.joinToString(" ")}\""
        return if (keepsPrefixStar(words.last())) "$body*" else body
    }

    private companion object {
        const val MIN_PREFIX_STAR_CHARS = 3
        val queryPartRegex = Regex("\"([^\"]*)\"|(\\S+)")
        val BOOLEAN_OPERATORS = setOf("AND", "OR", "NOT")
    }
}
