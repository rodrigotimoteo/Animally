package com.github.rodrigotimoteo.animally.presentation.consultation.view

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
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListUiState
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Consultation list embedded in the Medical tab of the patient detail screen.
 *
 * @param patientId The id of the patient whose consultations are listed.
 * @param modifier Optional modifier.
 * @param viewModel The [ConsultationListViewModel] for this screen.
 */
@Composable
fun ConsultationListScreen(
    patientId: Long,
    modifier: Modifier = Modifier,
    viewModel: ConsultationListViewModel = koinViewModel { parametersOf(patientId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConsultationListContent(
        uiState = uiState,
        modifier = modifier,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
    )
}

@Composable
private fun ConsultationListContent(
    uiState: ConsultationListUiState,
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
                text = "Consultations",
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
            uiState.consultations.isEmpty() ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No consultations yet", style = MaterialTheme.typography.bodyLarge)
                }
            else -> ConsultationList(uiState.consultations, onEditClick)
        }
    }
}

@Composable
private fun ConsultationList(
    consultations: List<Consultation>,
    onEditClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(consultations, key = { it.id }) { consultation ->
            ConsultationCard(consultation, onEditClick)
        }
    }
}

@Composable
private fun ConsultationCard(
    consultation: Consultation,
    onEditClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = { onEditClick(consultation.id) }, modifier = cardModifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(consultation.date.toString(), style = MaterialTheme.typography.titleMedium)
            consultation.subjective.takeIf { it.isNotBlank() }?.let {
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
