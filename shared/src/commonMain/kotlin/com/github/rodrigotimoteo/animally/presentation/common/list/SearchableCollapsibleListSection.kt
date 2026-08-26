package com.github.rodrigotimoteo.animally.presentation.common.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState

/** Renders the shared title, search control, and add action for a record list. */
@Composable
fun SearchableListHeader(
    title: String,
    displayState: ListDisplayState,
    onAddClick: () -> Unit,
    displayActions: ListDisplayActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (displayState.isSearchVisible) {
            OutlinedTextField(
                value = displayState.searchQuery.orEmpty(),
                onValueChange = displayActions.onSearchQueryChange,
                placeholder = { Text("Search") },
                singleLine = true,
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Search $title" },
                trailingIcon = {
                    TextButton(
                        onClick = displayActions.onCloseSearch,
                        modifier = Modifier.semantics { contentDescription = "Close search $title" },
                    ) {
                        Text("Close")
                    }
                },
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = displayActions.onSearchClick,
                modifier = Modifier.semantics { contentDescription = "Search $title" },
            ) {
                Text("Search")
            }
        }
        Button(onClick = onAddClick) {
            Text("Add")
        }
    }
}

/** Renders records, the no-match state, and the expand/collapse action. */
@Composable
fun <T> CollapsibleListContent(
    listState: CollapsibleListState<T>,
    displayActions: ListDisplayActions,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (listState.visibleItems.isEmpty()) {
            EmptyState(
                title = "No matches",
                message = "Try a different search term.",
                symbol = "🔍",
                onActionLabel = "Clear search",
                onAction = displayActions.onCloseSearch,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(listState.visibleItems, key = itemKey) { item ->
                    itemContent(item)
                }
            }
        }

        if (listState.filteredItemCount > COLLAPSED_LIST_LIMIT && listState.displayState.searchQuery.isNullOrBlank()) {
            TextButton(
                onClick = displayActions.onToggleExpanded,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .semantics {
                            contentDescription =
                                if (listState.displayState.isExpanded) "Show fewer records" else "Show all records"
                        },
            ) {
                Text(
                    if (listState.displayState.isExpanded) {
                        "Show less"
                    } else {
                        "Show all ${listState.filteredItemCount}"
                    },
                )
            }
        }
    }
}
