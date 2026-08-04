package com.github.rodrigotimoteo.animally.presentation.gestation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.CalculateGestationUseCase
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationFormState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private val STATUS_OPTIONS = listOf("Active", "Completed", "Failed")

/**
 * Screen for creating or editing a gestation record.
 *
 * @param viewModel The [GestationEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestationEditScreen(
    viewModel: GestationEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect == EditEffect.Saved) {
                viewModel.popBackStack()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Gestation" else "Add Gestation") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        GestationEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun GestationEditForm(
    viewModel: GestationEditViewModel,
    formState: GestationFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            GestationBreedingFields(viewModel, form)
            GestationStatusFields(viewModel, form)
            GestationFollowUpFields(viewModel, form)
            Button(
                onClick = viewModel::save,
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(if (form.isEditing) "Save" else "Create")
            }
        }
    }
}

@Composable
private fun GestationBreedingFields(
    viewModel: GestationEditViewModel,
    form: GestationFormState,
) {
    OutlinedTextField(
        value = form.breedingDate,
        onValueChange = viewModel::onBreedingDateChange,
        label = { Text("Breeding Date (yyyy-MM-dd) *") },
        isError = form.breedingDateError != null,
        supportingText = { form.breedingDateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    GestationProgressPreview(form)
}

@Composable
private fun GestationStatusFields(
    viewModel: GestationEditViewModel,
    form: GestationFormState,
) {
    Text("Status *", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STATUS_OPTIONS.forEach { status ->
            FilterChip(
                selected = form.status == status,
                onClick = { viewModel.onStatusChange(status) },
                label = { Text(status) },
            )
        }
    }
    form.statusError?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun GestationFollowUpFields(
    viewModel: GestationEditViewModel,
    form: GestationFormState,
) {
    OutlinedTextField(
        value = form.fetalCount.orEmpty(),
        onValueChange = viewModel::onFetalCountChange,
        label = { Text("Fetal Count") },
        isError = form.fetalCountError != null,
        supportingText = { form.fetalCountError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.lastCheckDate.orEmpty(),
        onValueChange = viewModel::onLastCheckDateChange,
        label = { Text("Last Check Date (yyyy-MM-dd)") },
        isError = form.lastCheckDateError != null,
        supportingText = { form.lastCheckDateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.notes.orEmpty(),
        onValueChange = viewModel::onNotesChange,
        label = { Text("Notes") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GestationProgressPreview(form: GestationFormState) {
    val breedingDate = runCatching { LocalDate.parse(form.breedingDate) }.getOrNull() ?: return
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val progress = CalculateGestationUseCase()(breedingDate, today)
    Text(
        text = "Due: ${progress.expectedDueDate} \u00B7 Day ${progress.gestationDays}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}
