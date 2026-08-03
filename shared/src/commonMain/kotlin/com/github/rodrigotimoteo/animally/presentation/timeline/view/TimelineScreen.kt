package com.github.rodrigotimoteo.animally.presentation.timeline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry
import com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineGroup
import com.github.rodrigotimoteo.animally.presentation.theme.successColorLight
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineUiState
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import kotlinx.datetime.LocalDate

private val ClinicalAmber = Color(0xFFC17817)
private val DiagnosticsPurple = Color(0xFF7E57C2)
private val ProceduresRed = Color(0xFFBA1A1A)
private val ReproductionPink = Color(0xFFD81B60)
private val DefaultSage = Color(0xFF6B9080)

/**
 * Screen displaying the timeline feed for a patient or globally.
 *
 * @param viewModel The [TimelineViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
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

    val title = if (uiState.patientId != null) "Timeline" else "All Patients"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (uiState.patientId != null) {
                        TextButton(onClick = viewModel::onBack) {
                            Text("Back")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        TimelineContent(
            uiState = uiState,
            onEntryClick = viewModel::onEntryClick,
            onRetry = viewModel::load,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun TimelineContent(
    uiState: TimelineUiState,
    onEntryClick: (recordType: String, patientId: Long, recordId: Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.errorMessage != null && uiState.groups.isEmpty() ->
                ErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
            uiState.groups.isEmpty() ->
                EmptyState(modifier = Modifier.align(Alignment.Center))
            else ->
                TimelineList(
                    groups = uiState.groups,
                    isGlobal = uiState.patientId == null,
                    onEntryClick = onEntryClick,
                )
        }
    }
}

@Composable
private fun TimelineList(
    groups: List<TimelineGroup>,
    isGlobal: Boolean,
    onEntryClick: (recordType: String, patientId: Long, recordId: Long) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.date}") {
                DateHeader(group.date)
            }
            items(
                items = group.entries,
                key = { entry -> "${entry.recordType}-${entry.recordId}" },
            ) { entry ->
                TimelineEntryRow(
                    entry = entry,
                    showPatientName = isGlobal,
                    onClick = { onEntryClick(entry.recordType, entry.patientId, entry.recordId) },
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val headerModifier =
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    Column(modifier = headerModifier) {
        Text(
            text = date.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineEntryRow(
    entry: TimelineEntry,
    showPatientName: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = accentForRecordType(entry.recordType)
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    val barModifier =
        Modifier
            .width(4.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(accentColor)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = barModifier)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (showPatientName) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.patientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (entry.subtitle.isNotBlank()) {
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.date.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No events yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Timeline entries will appear here as records are added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Maps a timeline record type to a color accent.
 *
 * Grouped by category:
 * - Prevention (teal/green): Vaccination, Deworming, Dentistry, Farrier
 * - Clinical (amber): Weight, Consultation, Medication, Controlled Substance
 * - Diagnostics (purple): Lab Result, Imaging, Lameness
 * - Procedures (red): Surgery
 * - Reproduction (pink): Reproduction, Ultrasound, Gestation, Repro Medication
 */
@Composable
private fun accentForRecordType(recordType: String): Color =
    when (recordType) {
        "Vaccination", "Deworming", "Dentistry", "Farrier" -> successColorLight
        "Weight", "Consultation", "Medication", "Controlled Substance" -> ClinicalAmber
        "Lab Result", "Imaging", "Lameness" -> DiagnosticsPurple
        "Surgery" -> ProceduresRed
        "Reproduction", "Ultrasound", "Gestation", "Repro Medication" -> ReproductionPink
        else -> DefaultSage
    }
