package com.github.rodrigotimoteo.animally.presentation.common.list

/** Maximum number of records shown before a list is expanded by the user. */
const val COLLAPSED_LIST_LIMIT = 5

/**
 * Presentation state shared by patient-record lists.
 *
 * A `null` [searchQuery] means that the search field is closed. An empty query means that the
 * field is open but no filter has been entered yet.
 */
data class ListDisplayState(
    val searchQuery: String? = null,
    val isExpanded: Boolean = false,
) {
    /** Whether the inline search field is currently visible. */
    val isSearchVisible: Boolean
        get() = searchQuery != null
}

/** Filters records when every whitespace-delimited search term matches the record text. */
fun <T> List<T>.filterBySearch(
    query: String?,
    searchableText: (T) -> String,
): List<T> {
    val terms =
        query
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.filter(String::isNotBlank)
            .orEmpty()
    if (terms.isEmpty()) return this

    return filter { record ->
        val text = searchableText(record)
        terms.all { term -> text.contains(term, ignoreCase = true) }
    }
}

/** Applies the default collapsed presentation while keeping filtered search results complete. */
fun <T> List<T>.visibleForListDisplay(displayState: ListDisplayState): List<T> =
    if (displayState.isExpanded || !displayState.searchQuery.isNullOrBlank()) {
        this
    } else {
        take(COLLAPSED_LIST_LIMIT)
    }
