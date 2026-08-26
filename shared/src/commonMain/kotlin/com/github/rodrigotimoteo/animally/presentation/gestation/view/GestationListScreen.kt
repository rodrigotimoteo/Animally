package com.github.rodrigotimoteo.animally.presentation.gestation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListContent
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListState
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayActions
import com.github.rodrigotimoteo.animally.presentation.common.list.RecordListActions
import com.github.rodrigotimoteo.animally.presentation.common.list.SearchableListHeader
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListUiState
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListViewModel

/**
 * Gestation list embedded in the Reproduction tab of the patient detail screen.
 *
 * @param viewModel The [GestationListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun GestationListScreen(
    viewModel: GestationListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GestationListContent(
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
private fun GestationListContent(
    uiState: GestationListUiState,
    modifier: Modifier,
    actions: RecordListActions,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchableListHeader(
            title = "Gestations",
            displayState = uiState.displayState,
            onAddClick = actions.onAddClick,
            displayActions = actions.displayActions,
        )
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            uiState.errorMessage != null ->
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = actions.errorHandlers.onRetry,
                    onDismiss = actions.errorHandlers.onDismiss,
                    modifier = Modifier.weight(1f),
                )
            uiState.records.isEmpty() ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No gestations yet", style = MaterialTheme.typography.bodyLarge)
                }
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
                    itemContent = { gestation -> GestationCard(gestation, actions.onItemClick) },
                )
        }
    }
}

@Composable
private fun GestationCard(
    gestation: Gestation,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(gestation.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(gestation.status, style = MaterialTheme.typography.titleMedium)
            Text("Bred: ${gestation.breedingDate}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Due: ${gestation.expectedDueDate} \u00B7 Day ${gestation.gestationDays}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
