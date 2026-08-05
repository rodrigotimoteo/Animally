package com.github.rodrigotimoteo.animally.presentation.deworming.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListUiState
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListViewModel

/**
 * Deworming list embedded in the Medical tab of the patient detail screen.
 *
 * @param viewModel The [DewormingListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun DewormingListScreen(
    viewModel: DewormingListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DewormingListContent(
        uiState = uiState,
        modifier = modifier,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        errorHandlers = ListErrorHandlers(onRetry = viewModel::load, onDismiss = viewModel::onDismissError),
    )
}

@Composable
private fun DewormingListContent(
    uiState: DewormingListUiState,
    modifier: Modifier,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    errorHandlers: ListErrorHandlers,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Dewormings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onAddClick) {
                Text("Add")
            }
        }
        when {
            uiState.isLoading ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            uiState.errorMessage != null ->
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = errorHandlers.onRetry,
                    onDismiss = errorHandlers.onDismiss,
                )
            uiState.records.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No dewormings yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> DewormingList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun DewormingList(
    dewormings: List<Deworming>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(dewormings, key = { it.id }) { deworming ->
            DewormingCard(deworming, onEditClick)
        }
    }
}

@Composable
private fun DewormingCard(
    deworming: Deworming,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(deworming.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(deworming.product, style = MaterialTheme.typography.titleMedium)
            Text(deworming.dateAdministered.toString(), style = MaterialTheme.typography.bodyMedium)
            deworming.notes?.takeIf { it.isNotBlank() }?.let {
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
