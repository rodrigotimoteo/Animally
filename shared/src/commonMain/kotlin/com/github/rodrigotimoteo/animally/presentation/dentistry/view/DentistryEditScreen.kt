package com.github.rodrigotimoteo.animally.presentation.dentistry.view

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
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryFormState

/**
 * Screen for creating or editing a dentistry check record.
 *
 * @param viewModel The [DentistryEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentistryEditScreen(
    viewModel: DentistryEditViewModel,
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
                title = { Text(if (formState?.isEditing == true) "Edit Dentistry" else "Add Dentistry") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        DentistryEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun DentistryEditForm(
    viewModel: DentistryEditViewModel,
    formState: DentistryFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            DentistryBasicFields(viewModel, form)
            DentistryScheduleFields(viewModel, form)
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
private fun DentistryBasicFields(
    viewModel: DentistryEditViewModel,
    form: DentistryFormState,
) {
    OutlinedTextField(
        value = form.date,
        onValueChange = viewModel::onDateChange,
        label = { Text("Date (yyyy-MM-dd) *") },
        isError = form.dateError != null,
        supportingText = { form.dateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.findings.orEmpty(),
        onValueChange = viewModel::onFindingsChange,
        label = { Text("Findings") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.treatment.orEmpty(),
        onValueChange = viewModel::onTreatmentChange,
        label = { Text("Treatment") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DentistryScheduleFields(
    viewModel: DentistryEditViewModel,
    form: DentistryFormState,
) {
    OutlinedTextField(
        value = form.vetName.orEmpty(),
        onValueChange = viewModel::onVetNameChange,
        label = { Text("Vet Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.nextDueDate.orEmpty(),
        onValueChange = viewModel::onNextDueDateChange,
        label = { Text("Next Due Date (yyyy-MM-dd)") },
        isError = form.nextDueDateError != null,
        supportingText = { form.nextDueDateError?.let { Text(it) } },
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
