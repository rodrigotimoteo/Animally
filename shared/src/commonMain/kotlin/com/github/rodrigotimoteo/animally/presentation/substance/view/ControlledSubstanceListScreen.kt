package com.github.rodrigotimoteo.animally.presentation.substance.view

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
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
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
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
    )
}

@Composable
private fun ControlledSubstanceListContent(
    uiState: ControlledSubstanceListUiState,
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
                text = "Controlled Substances",
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
                    Text("No controlled-substance records yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> ControlledSubstanceList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun ControlledSubstanceList(
    records: List<ControlledSubstance>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(records, key = { it.id }) { record ->
            ControlledSubstanceCard(record, onEditClick)
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
