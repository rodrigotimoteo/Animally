package com.github.rodrigotimoteo.animally.presentation.patientList.view

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Patients") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PatientListContent(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onAddClick = viewModel::onAddClick,
            onPatientClick = viewModel::onPatientClick,
            onDeleteClick = viewModel::onDeleteClick,
        )
    }
}

@Composable
private fun PatientListContent(
    uiState: PatientListUiState,
    modifier: Modifier,
    onAddClick: () -> Unit,
    onPatientClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.patients.isEmpty() -> EmptyPatients(onAddClick)
            else -> PatientList(uiState.patients, onPatientClick, onDeleteClick)
        }
    }
}

@Composable
private fun EmptyPatients(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No patients yet", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAddClick, modifier = Modifier.padding(top = 12.dp)) {
            Text("Add patient")
        }
    }
}

@Composable
private fun PatientList(
    patients: List<Patient>,
    onPatientClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(patients, key = { it.id }) { patient ->
            PatientCard(patient, onPatientClick, onDeleteClick)
        }
    }
}

@Composable
private fun PatientCard(
    patient: Patient,
    onPatientClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
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
            Button(onClick = { onDeleteClick(patient.id) }) {
                Text("Delete")
            }
        }
    }
}
