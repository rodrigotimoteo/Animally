package com.github.rodrigotimoteo.animally.presentation.medication.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.EditEffect
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationFormState

/**
 * Screen for creating or editing a medication.
 *
 * @param viewModel The [MedicationEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditScreen(
    viewModel: MedicationEditViewModel,
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
                title = { Text(if (formState?.isEditing == true) "Edit Medication" else "Add Medication") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        MedicationEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun MedicationEditForm(
    viewModel: MedicationEditViewModel,
    formState: MedicationFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            MedicationBasicFields(viewModel, form)
            MedicationScheduleFields(viewModel, form)
            MedicationMetaFields(viewModel, form)
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
private fun MedicationBasicFields(
    viewModel: MedicationEditViewModel,
    form: MedicationFormState,
) {
    OutlinedTextField(
        value = form.name,
        onValueChange = viewModel::onNameChange,
        label = { Text("Name *") },
        isError = form.nameError != null,
        supportingText = { form.nameError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.dosage,
        onValueChange = viewModel::onDosageChange,
        label = { Text("Dosage *") },
        isError = form.dosageError != null,
        supportingText = { form.dosageError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.route.orEmpty(),
        onValueChange = viewModel::onRouteChange,
        label = { Text("Route") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.frequency.orEmpty(),
        onValueChange = viewModel::onFrequencyChange,
        label = { Text("Frequency") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MedicationScheduleFields(
    viewModel: MedicationEditViewModel,
    form: MedicationFormState,
) {
    OutlinedTextField(
        value = form.startDate.orEmpty(),
        onValueChange = viewModel::onStartDateChange,
        label = { Text("Start Date (yyyy-MM-dd)") },
        isError = form.startDateError != null,
        supportingText = { form.startDateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.endDate.orEmpty(),
        onValueChange = viewModel::onEndDateChange,
        label = { Text("End Date (yyyy-MM-dd)") },
        isError = form.endDateError != null,
        supportingText = { form.endDateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MedicationMetaFields(
    viewModel: MedicationEditViewModel,
    form: MedicationFormState,
) {
    OutlinedTextField(
        value = form.prescribedBy.orEmpty(),
        onValueChange = viewModel::onPrescribedByChange,
        label = { Text("Prescribed By") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.notes.orEmpty(),
        onValueChange = viewModel::onNotesChange,
        label = { Text("Notes") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}
