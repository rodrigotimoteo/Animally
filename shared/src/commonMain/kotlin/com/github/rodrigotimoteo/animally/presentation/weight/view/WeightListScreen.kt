package com.github.rodrigotimoteo.animally.presentation.weight.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListContent
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListState
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayActions
import com.github.rodrigotimoteo.animally.presentation.common.list.RecordListActions
import com.github.rodrigotimoteo.animally.presentation.common.list.SearchableListHeader
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.common.state.LoadingState
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListUiState
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListViewModel

/**
 * Weight list embedded in the patient detail screen.
 *
 * @param viewModel The [WeightListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun WeightListScreen(
    viewModel: WeightListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WeightListContent(
        uiState = uiState,
        modifier = modifier,
        actions =
            RecordListActions(
                onAddClick = viewModel::onAddClick,
                onItemClick = viewModel::onEditClick,
                displayActions =
                    ListDisplayActions(
                        onSearchClick = viewModel::onSearchClick,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onCloseSearch = viewModel::onCloseSearch,
                        onToggleExpanded = viewModel::onToggleExpanded,
                    ),
                errorHandlers = ListErrorHandlers(onRetry = viewModel::load, onDismiss = viewModel::onDismissError),
            ),
    )
}

@Composable
private fun WeightListContent(
    uiState: WeightListUiState,
    modifier: Modifier,
    actions: RecordListActions,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchableListHeader(
            title = "Weight",
            displayState = uiState.displayState,
            onAddClick = actions.onAddClick,
            displayActions = actions.displayActions,
        )
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.weight(1f))
            uiState.errorMessage != null ->
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = actions.errorHandlers.onRetry,
                    onDismiss = actions.errorHandlers.onDismiss,
                    modifier = Modifier.weight(1f),
                )
            uiState.records.isEmpty() ->
                EmptyState(title = "No weight records yet", modifier = Modifier.weight(1f))
            else ->
                CollapsibleListContent(
                    listState =
                        CollapsibleListState(
                            visibleItems = uiState.visibleRecords,
                            filteredItemCount = uiState.filteredRecords.size,
                            displayState = uiState.displayState,
                        ),
                    displayActions = actions.displayActions,
                    itemKey = { it.id },
                    modifier = Modifier.weight(1f),
                    itemContent = { record -> WeightCard(record, actions.onItemClick) },
                )
        }
    }
}

@Composable
private fun WeightCard(
    record: Weight,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(record.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "${record.weightKg} kg — ${record.date}",
                style = MaterialTheme.typography.titleMedium,
            )
            record.notes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
