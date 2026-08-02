package com.github.rodrigotimoteo.animally.presentation.gestation.view

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
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
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
    )
}

@Composable
private fun GestationListContent(
    uiState: GestationListUiState,
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
                text = "Gestations",
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
                    Text("No gestations yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> GestationList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun GestationList(
    gestations: List<Gestation>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(gestations, key = { it.id }) { gestation ->
            GestationCard(gestation, onEditClick)
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
