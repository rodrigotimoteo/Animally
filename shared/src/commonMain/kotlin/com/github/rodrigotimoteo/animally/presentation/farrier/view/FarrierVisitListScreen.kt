package com.github.rodrigotimoteo.animally.presentation.farrier.view

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
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListUiState
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListViewModel

/**
 * Farrier visit list embedded in the Preventive tab of the patient detail screen.
 *
 * @param viewModel The [FarrierVisitListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun FarrierVisitListScreen(
    viewModel: FarrierVisitListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FarrierVisitListContent(
        uiState = uiState,
        modifier = modifier,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
    )
}

@Composable
private fun FarrierVisitListContent(
    uiState: FarrierVisitListUiState,
    modifier: Modifier,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Farrier Visits",
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(uiState.errorMessage, style = MaterialTheme.typography.bodyLarge)
                }
            uiState.records.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No farrier visits yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> FarrierVisitList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun FarrierVisitList(
    visits: List<FarrierVisit>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(visits, key = { it.id }) { visit ->
            FarrierVisitCard(visit, onEditClick)
        }
    }
}

@Composable
private fun FarrierVisitCard(
    visit: FarrierVisit,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(visit.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(visit.date.toString(), style = MaterialTheme.typography.titleMedium)
            visit.trimOrShoe?.takeIf { it.isNotBlank() }?.let {
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
