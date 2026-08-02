package com.github.rodrigotimoteo.animally.presentation.ownerDetail.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailUiState
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel

/**
 * Screen displaying the detail of a single owner.
 *
 * @param viewModel The [OwnerDetailViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDetailScreen(
    viewModel: OwnerDetailViewModel,
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
        topBar = {
            TopAppBar(
                title = { Text("Owner Detail") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::onEditClick, enabled = uiState.owner != null) {
                        Text("Edit")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        OwnerDetailContent(uiState = uiState, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun OwnerDetailContent(
    uiState: OwnerDetailUiState,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.owner == null -> {
                Text(
                    "Owner not found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> OwnerDetail(checkNotNull(uiState.owner), uiState.patients)
        }
    }
}

@Composable
private fun OwnerDetail(
    owner: Owner,
    patients: List<Patient>,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            OwnerInfoCard(owner)
        }
        item {
            val titleModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            Text(
                "Linked Patients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = titleModifier,
            )
        }
        if (patients.isEmpty()) {
            item {
                val emptyModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                Text(
                    "No linked patients",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = emptyModifier,
                )
            }
        } else {
            items(patients, key = { it.id }) { patient ->
                PatientRow(patient.name)
            }
        }
    }
}

@Composable
private fun OwnerInfoCard(owner: Owner) {
    val cardModifier = Modifier.fillMaxWidth().padding(16.dp)
    Card(modifier = cardModifier) {
        Column(Modifier.padding(16.dp)) {
            Text(owner.name, style = MaterialTheme.typography.headlineSmall)
            owner.phone?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            owner.email?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            owner.address?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun PatientRow(name: String) {
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Row(modifier = rowModifier) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
