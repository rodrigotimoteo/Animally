package com.github.rodrigotimoteo.animally.presentation.patientDetail.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.presentation.common.glass.GlassTopAppBar
import com.github.rodrigotimoteo.animally.presentation.common.glass.hazeSourceFrom
import com.github.rodrigotimoteo.animally.presentation.common.glass.rememberHazeState
import com.github.rodrigotimoteo.animally.presentation.common.layout.WindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.layout.withWindowSizeClass
import com.github.rodrigotimoteo.animally.presentation.common.state.EmptyState
import com.github.rodrigotimoteo.animally.presentation.common.state.LoadingState
import com.github.rodrigotimoteo.animally.presentation.consultation.view.ConsultationListScreen
import com.github.rodrigotimoteo.animally.presentation.dentistry.view.DentistryListScreen
import com.github.rodrigotimoteo.animally.presentation.deworming.view.DewormingListScreen
import com.github.rodrigotimoteo.animally.presentation.farrier.view.FarrierVisitListScreen
import com.github.rodrigotimoteo.animally.presentation.gestation.view.GestationListScreen
import com.github.rodrigotimoteo.animally.presentation.imaging.view.ImagingListScreen
import com.github.rodrigotimoteo.animally.presentation.labresult.view.LabResultListScreen
import com.github.rodrigotimoteo.animally.presentation.lameness.view.LamenessListScreen
import com.github.rodrigotimoteo.animally.presentation.medication.view.MedicationListScreen
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailUiState
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.view.ReproductionEventListScreen
import com.github.rodrigotimoteo.animally.presentation.repromedication.view.ReproMedicationListScreen
import com.github.rodrigotimoteo.animally.presentation.substance.view.ControlledSubstanceListScreen
import com.github.rodrigotimoteo.animally.presentation.surgery.view.SurgeryListScreen
import com.github.rodrigotimoteo.animally.presentation.ultrasound.view.UltrasoundListScreen
import com.github.rodrigotimoteo.animally.presentation.vaccination.view.VaccinationListScreen
import com.github.rodrigotimoteo.animally.presentation.weight.view.WeightListScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val hazeState = rememberHazeState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(uiState.patient?.name ?: "Patient") },
                hazeState = hazeState,
                navigationIcon = {
                    TextButton(
                        onClick = viewModel::onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" },
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onTimelineClick,
                        enabled = uiState.patient != null,
                        modifier = Modifier.semantics { contentDescription = "Timeline" },
                    ) {
                        Text("Timeline")
                    }
                    TextButton(
                        onClick = viewModel::onCustomRemindersClick,
                        enabled = uiState.patient != null,
                        modifier = Modifier.semantics { contentDescription = "Reminders" },
                    ) {
                        Text("Reminders")
                    }
                    TextButton(
                        onClick = viewModel::onEditClick,
                        enabled = uiState.patient != null,
                        modifier = Modifier.semantics { contentDescription = "Edit patient" },
                    ) {
                        Text("Edit")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PatientDetailContent(
            uiState = uiState,
            hazeState = hazeState,
            onAnamneseClick = viewModel::onAnamneseClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PatientDetailContent(
    uiState: PatientDetailUiState,
    hazeState: dev.chrisbanes.haze.HazeState,
    onAnamneseClick: () -> Unit,
    modifier: Modifier,
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.patient == null ->
            EmptyState(
                title = "Patient not found",
                message = "This patient may have been removed.",
                modifier = modifier,
            )
        else ->
            withWindowSizeClass { sizeClass ->
                PatientTabs(
                    uiState = uiState,
                    hazeState = hazeState,
                    sizeClass = sizeClass,
                    onAnamneseClick = onAnamneseClick,
                    modifier = modifier,
                )
            }
    }
}

@Composable
private fun PatientTabs(
    uiState: PatientDetailUiState,
    hazeState: dev.chrisbanes.haze.HazeState,
    sizeClass: WindowSizeClass,
    onAnamneseClick: () -> Unit,
    modifier: Modifier,
) {
    val tabs = PatientTab.entries
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val patient = checkNotNull(uiState.patient)
    val contentModifier = modifier.fillMaxSize().hazeSourceFrom(hazeState)

    when (sizeClass) {
        WindowSizeClass.Compact, WindowSizeClass.Medium ->
            Column(contentModifier) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = tab.ordinal == selectedTab,
                            onClick = { selectedTab = tab.ordinal },
                            text = { Text(tab.label) },
                        )
                    }
                }
                TabContent(selectedTab, tabs, patient, uiState.ownerName, onAnamneseClick)
            }
        WindowSizeClass.Expanded ->
            Row(contentModifier) {
                Column(modifier = Modifier.widthIn(min = 200.dp, max = 280.dp).fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = tab.ordinal == selectedTab,
                                onClick = { selectedTab = tab.ordinal },
                                text = { Text(tab.label) },
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    TabContent(selectedTab, tabs, patient, uiState.ownerName, onAnamneseClick)
                }
            }
    }
}

@Composable
private fun TabContent(
    selectedTab: Int,
    tabs: List<PatientTab>,
    patient: Patient,
    ownerName: String?,
    onAnamneseClick: () -> Unit,
) {
    when (tabs[selectedTab]) {
        PatientTab.Overview -> OverviewTab(patient, ownerName, onAnamneseClick)
        PatientTab.Medical -> MedicalTab(patient.id)
        PatientTab.Preventive -> PreventiveTab(patient.id)
        PatientTab.Reproduction -> ReproductionTab(patient.id)
        PatientTab.DiagnosticsFiles -> DiagnosticsFilesTab(patient.id)
    }
}

@Composable
private fun OverviewTab(
    patient: Patient,
    ownerName: String?,
    onAnamneseClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        ) {
            PatientInfoCard(patient, ownerName)
            AnamneseCard(onAnamneseClick)
        }
        WeightListScreen(
            viewModel = koinViewModel { parametersOf(patient.id) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnamneseCard(onClick: () -> Unit) {
    val cardModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    Card(onClick = onClick, modifier = cardModifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Anamnese", style = MaterialTheme.typography.titleMedium)
                Text("General history, chronic conditions, allergies", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Edit", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MedicalTab(patientId: Long) {
    Column(Modifier.fillMaxSize()) {
        ConsultationListScreen(patientId = patientId, modifier = Modifier.weight(1f))
        LamenessListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        SurgeryListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        MedicationListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        ControlledSubstanceListScreen(
            viewModel = koinViewModel { parametersOf(patientId) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PreventiveTab(patientId: Long) {
    Column(Modifier.fillMaxSize()) {
        VaccinationListScreen(patientId = patientId, modifier = Modifier.weight(1f))
        DewormingListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        DentistryListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        FarrierVisitListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ReproductionTab(patientId: Long) {
    Column(Modifier.fillMaxSize()) {
        ReproductionEventListScreen(
            viewModel = koinViewModel { parametersOf(patientId) },
            modifier = Modifier.weight(1f),
        )
        UltrasoundListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        GestationListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        ReproMedicationListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DiagnosticsFilesTab(patientId: Long) {
    Column(Modifier.fillMaxSize()) {
        LabResultListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
        ImagingListScreen(viewModel = koinViewModel { parametersOf(patientId) }, modifier = Modifier.weight(1f))
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
