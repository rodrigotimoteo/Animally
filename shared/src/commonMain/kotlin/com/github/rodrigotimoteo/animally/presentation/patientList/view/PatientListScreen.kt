package com.github.rodrigotimoteo.animally.presentation.patientList.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.presentation.common.glass.GlassTopAppBar
import com.github.rodrigotimoteo.animally.presentation.common.glass.LocalHazeState
import com.github.rodrigotimoteo.animally.presentation.common.glass.hazeSourceFrom
import com.github.rodrigotimoteo.animally.presentation.common.glass.rememberHazeState
import com.github.rodrigotimoteo.animally.presentation.common.layout.WindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.layout.withWindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState
import com.github.rodrigotimoteo.animally.presentation.common.state.ErrorState
import com.github.rodrigotimoteo.animally.presentation.common.state.ListActions
import com.github.rodrigotimoteo.animally.presentation.common.state.ListErrorHandlers
import com.github.rodrigotimoteo.animally.presentation.common.state.LoadingState
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListUiState
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel

/**
 * Screen displaying the list of patients.
 *
 * @param viewModel The [PatientListViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    viewModel: PatientListViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    val actions =
        ListActions(
            onAddClick = viewModel::onAddClick,
            onItemClick = viewModel::onPatientClick,
            onDeleteClick = viewModel::onDeleteClick,
        )
    val errorHandlers =
        ListErrorHandlers(
            onRetry = viewModel::loadPatients,
            onDismiss = viewModel::onDismissError,
        )

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Scaffold(
            modifier = modifier,
            topBar = {
                GlassTopAppBar(
                    title = { Text("Patients") },
                    hazeState = hazeState,
                    actions = {
                        TextButton(onClick = viewModel::onSearchClick) {
                            Text("Search")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::onAddClick,
                    modifier = Modifier.semantics { contentDescription = "Add patient" },
                ) {
                    Text("+")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            PatientListContent(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
                actions = actions,
                errorHandlers = errorHandlers,
            )
        }
    }
}

@Composable
private fun PatientListContent(
    uiState: PatientListUiState,
    modifier: Modifier,
    actions: ListActions,
    errorHandlers: ListErrorHandlers,
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.errorMessage != null && uiState.patients.isEmpty() ->
            ErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = errorHandlers.onRetry,
                onDismiss = errorHandlers.onDismiss,
                modifier = modifier,
            )
        uiState.patients.isEmpty() ->
            EmptyState(
                title = "No patients yet",
                message = "Add your first horse to start tracking care.",
                symbol = "🐴",
                onActionLabel = "Add patient",
                onAction = actions.onAddClick,
                modifier = modifier,
            )
        else ->
            withWindowSizeClass { sizeClass ->
                PatientList(
                    patients = uiState.patients,
                    sizeClass = sizeClass,
                    modifier = modifier,
                    onPatientClick = actions.onItemClick,
                    onDeleteClick = actions.onDeleteClick,
                )
            }
    }
}

@Composable
private fun PatientList(
    patients: List<Patient>,
    sizeClass: WindowSizeClass,
    modifier: Modifier,
    onPatientClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val listModifier = modifier.fillMaxSize().hazeSourceFrom(LocalHazeState.current)
    when (sizeClass) {
        WindowSizeClass.Compact, WindowSizeClass.Medium ->
            LazyColumn(listModifier) {
                items(patients, key = { it.id }) { patient ->
                    PatientCard(patient, onPatientClick, onDeleteClick)
                }
            }
        WindowSizeClass.Expanded ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = listModifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(patients, key = { it.id }) { patient ->
                    PatientCard(patient, onPatientClick, onDeleteClick)
                }
            }
    }
}

@Composable
private fun PatientCard(
    patient: Patient,
    onPatientClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val cardModifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Patient ${patient.name}"
            }
    Card(onClick = { onPatientClick(patient.id) }, modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.titleMedium)
                val supporting =
                    listOfNotNull(patient.breed, patient.microchipId)
                        .joinToString(" • ")
                if (supporting.isNotBlank()) {
                    Text(supporting, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = { onDeleteClick(patient.id) },
                modifier = Modifier.semantics { contentDescription = "Delete ${patient.name}" },
            ) {
                Text("Delete")
            }
        }
    }
}
