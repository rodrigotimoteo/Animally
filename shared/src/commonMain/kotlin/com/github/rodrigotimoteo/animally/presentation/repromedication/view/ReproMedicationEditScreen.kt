package com.github.rodrigotimoteo.animally.presentation.repromedication.view

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationFormState

/**
 * Screen for creating or editing a reproduction medication.
 *
 * @param viewModel The [ReproMedicationEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproMedicationEditScreen(
    viewModel: ReproMedicationEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

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
        ReproMedicationEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ReproMedicationEditForm(
    viewModel: ReproMedicationEditViewModel,
    formState: ReproMedicationFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            ReproMedicationBasicFields(viewModel, form)
            ReproMedicationMetaFields(viewModel, form)
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
private fun ReproMedicationBasicFields(
    viewModel: ReproMedicationEditViewModel,
    form: ReproMedicationFormState,
) {
    OutlinedTextField(
        value = form.medication,
        onValueChange = viewModel::onMedicationChange,
        label = { Text("Medication *") },
        isError = form.medicationError != null,
        supportingText = { form.medicationError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.dateAdministered,
        onValueChange = viewModel::onDateAdministeredChange,
        label = { Text("Date Administered (yyyy-MM-dd) *") },
        isError = form.dateError != null,
        supportingText = { form.dateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.dosage.orEmpty(),
        onValueChange = viewModel::onDosageChange,
        label = { Text("Dosage") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.purpose.orEmpty(),
        onValueChange = viewModel::onPurposeChange,
        label = { Text("Purpose") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReproMedicationMetaFields(
    viewModel: ReproMedicationEditViewModel,
    form: ReproMedicationFormState,
) {
    OutlinedTextField(
        value = form.vetName.orEmpty(),
        onValueChange = viewModel::onVetNameChange,
        label = { Text("Vet Name") },
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
