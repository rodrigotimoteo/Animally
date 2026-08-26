package com.github.rodrigotimoteo.animally.presentation.common.list

/** Callbacks used by searchable and expandable record-list controls. */
class ListDisplayActions(
    val onSearchClick: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onCloseSearch: () -> Unit,
    val onToggleExpanded: () -> Unit,
)

/** State needed to render a filtered, expandable record list. */
data class CollapsibleListState<T>(
    val visibleItems: List<T>,
    val filteredItemCount: Int,
    val displayState: ListDisplayState,
)
