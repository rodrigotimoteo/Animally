package com.github.rodrigotimoteo.animally.presentation.repromedication.view

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
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListUiState
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListViewModel

/**
 * Reproduction-medication list embedded in the Reproduction tab of the patient detail screen.
 *
 * @param viewModel The [ReproMedicationListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@Composable
fun ReproMedicationListScreen(
    viewModel: ReproMedicationListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReproMedicationListContent(
        uiState = uiState,
        modifier = modifier,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        errorHandlers = ListErrorHandlers(onRetry = viewModel::load, onDismiss = viewModel::onDismissError),
    )
}

@Composable
private fun ReproMedicationListContent(
    uiState: ReproMedicationListUiState,
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
                text = "Repro Medications",
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
                    Text("No reproduction medications yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> ReproMedicationList(uiState.records, onEditClick)
        }
    }
}

@Composable
private fun ReproMedicationList(
    medications: List<ReproMedication>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(medications, key = { it.id }) { medication ->
            ReproMedicationCard(medication, onEditClick)
        }
    }
}

@Composable
private fun ReproMedicationCard(
    medication: ReproMedication,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(medication.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(medication.medication, style = MaterialTheme.typography.titleMedium)
            Text(medication.dateAdministered.toString(), style = MaterialTheme.typography.bodyMedium)
            medication.dosage?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
