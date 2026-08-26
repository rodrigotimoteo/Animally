package com.github.rodrigotimoteo.animally.presentation.consultation.view

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
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListContent
import com.github.rodrigotimoteo.animally.presentation.common.list.CollapsibleListState
import com.github.rodrigotimoteo.animally.presentation.common.list.ListDisplayActions
import com.github.rodrigotimoteo.animally.presentation.common.list.RecordListActions
import com.github.rodrigotimoteo.animally.presentation.common.list.SearchableListHeader
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
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
private fun ConsultationListContent(
    uiState: ConsultationListUiState,
    modifier: Modifier,
    actions: RecordListActions,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchableListHeader(
            title = "Consultations",
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
            uiState.consultations.isEmpty() ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No consultations yet", style = MaterialTheme.typography.bodyLarge)
                }
            else ->
                CollapsibleListContent(
                    listState =
                        CollapsibleListState(
                            visibleItems = uiState.visibleConsultations,
                            filteredItemCount = uiState.filteredConsultations.size,
                            displayState = uiState.displayState,
                        ),
                    displayActions = actions.displayActions,
                    itemKey = { it.id },
                    modifier = Modifier.weight(1f),
                    itemContent = { consultation -> ConsultationCard(consultation, actions.onItemClick) },
                )
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
