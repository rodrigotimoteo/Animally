package com.github.rodrigotimoteo.animally.presentation.substance.view

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListContent
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListState
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayActions
import com.github.rodrigotimoteo.animally.presentation.common.list.RecordListActions
import com.github.rodrigotimoteo.animally.presentation.common.list.SearchableListHeader
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListUiState
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListViewModel

/**
 * Controlled-substance list embedded in the patient detail screen.
 *
 * @param viewModel The [ControlledSubstanceListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun ControlledSubstanceListScreen(
    viewModel: ControlledSubstanceListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ControlledSubstanceListContent(
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
private fun ControlledSubstanceListContent(
    uiState: ControlledSubstanceListUiState,
    modifier: Modifier,
    actions: RecordListActions,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchableListHeader(
            title = "Controlled Substances",
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
                    Text("No controlled-substance records yet", style = MaterialTheme.typography.bodyLarge)
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
                    itemContent = { record -> ControlledSubstanceCard(record, actions.onItemClick) },
                )
        }
    }
}

@Composable
private fun ControlledSubstanceCard(
    record: ControlledSubstance,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(record.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(record.drugName, style = MaterialTheme.typography.titleMedium)
            val detail = record.unit?.let { "${record.dose} $it" } ?: record.dose
            Text(
                text = "$detail · ${record.date}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
