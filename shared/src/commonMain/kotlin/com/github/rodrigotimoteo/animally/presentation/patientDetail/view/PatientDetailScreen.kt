package com.github.rodrigotimoteo.animally.presentation.patientDetail.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailUiState
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel

/**
 * The five top-level tabs of the patient detail screen, per ADR-0006.
 */
private enum class PatientTab(
    val label: String,
) {
    Overview("Overview"),
    Medical("Medical"),
    Preventive("Preventive"),
    Reproduction("Reproduction"),
    DiagnosticsFiles("Diagnostics/Files"),
}

/**
 * Screen displaying the detail of a single patient.
 *
 * @param viewModel The [PatientDetailViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    viewModel: PatientDetailViewModel,
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
                title = { Text(uiState.patient?.name ?: "Patient") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::onEditClick, enabled = uiState.patient != null) {
                        Text("Edit")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PatientDetailContent(uiState = uiState, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun PatientDetailContent(
    uiState: PatientDetailUiState,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.patient == null -> {
                Text(
                    "Patient not found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> PatientTabs(uiState)
        }
    }
}

@Composable
private fun PatientTabs(uiState: PatientDetailUiState) {
    val tabs = PatientTab.entries
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab.ordinal == selectedTab,
                    onClick = { selectedTab = tab.ordinal },
                    text = { Text(tab.label) },
                )
            }
        }
        when (tabs[selectedTab]) {
            PatientTab.Overview -> OverviewTab(checkNotNull(uiState.patient), uiState.ownerName)
            else -> PlaceholderTab()
        }
    }
}

@Composable
private fun OverviewTab(
    patient: Patient,
    ownerName: String?,
) {
    val scrollModifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = scrollModifier) {
        PatientInfoCard(patient, ownerName)
    }
}

@Composable
private fun PatientInfoCard(
    patient: Patient,
    ownerName: String?,
) {
    val cardModifier = Modifier.fillMaxWidth().padding(16.dp)
    Card(modifier = cardModifier) {
        Column(Modifier.padding(16.dp)) {
            Text(patient.name, style = MaterialTheme.typography.headlineSmall)
            InfoRow("Species", patient.species)
            patient.breed?.let { InfoRow("Breed", it) }
            patient.gender?.let { InfoRow("Gender", it) }
            patient.dateOfBirth?.let { InfoRow("Date of Birth", it.toString()) }
            patient.microchipId?.let { InfoRow("Microchip ID", it) }
            patient.ueln?.let { InfoRow("UELN", it) }
            patient.registrationNumber?.let { InfoRow("Registration Number", it) }
            patient.stableLocation?.let { InfoRow("Stable Location", it) }
            ownerName?.let { InfoRow("Owner", it) }
            patient.notes?.let { InfoRow("Notes", it) }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun PlaceholderTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Coming soon", style = MaterialTheme.typography.bodyLarge)
    }
}
