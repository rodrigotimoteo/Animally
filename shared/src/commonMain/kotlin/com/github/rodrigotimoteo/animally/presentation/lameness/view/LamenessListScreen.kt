package com.github.rodrigotimoteo.animally.presentation.lameness.view

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
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListUiState
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListViewModel

/**
 * Lameness list embedded in the patient detail screen.
 *
 * @param viewModel The [LamenessListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun LamenessListScreen(
    viewModel: LamenessListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LamenessListContent(
        uiState = uiState,
        modifier = modifier,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        errorHandlers = ListErrorHandlers(onRetry = viewModel::load, onDismiss = viewModel::onDismissError),
    )
}

@Composable
private fun LamenessListContent(
    uiState: LamenessListUiState,
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
                text = "Lameness",
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
                    Text("No lameness records yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> LamenessList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun LamenessList(
    records: List<Lameness>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(records, key = { it.id }) { record ->
            LamenessCard(record, onEditClick)
        }
    }
}

@Composable
private fun LamenessCard(
    record: Lameness,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(record.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(record.date.toString(), style = MaterialTheme.typography.titleMedium)
            val detail = "Grade ${record.gradeAAEP}"
            record.diagnosis?.let { diagnosis ->
                Text(
                    text = "$detail · $diagnosis",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (record.diagnosis == null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
