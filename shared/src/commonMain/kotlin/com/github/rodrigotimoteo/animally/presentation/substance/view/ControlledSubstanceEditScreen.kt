package com.github.rodrigotimoteo.animally.presentation.substance.view

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
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceEditViewModel
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceFormState

/**
 * Screen for creating or editing a controlled-substance record.
 *
 * @param viewModel The [ControlledSubstanceEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlledSubstanceEditScreen(
    viewModel: ControlledSubstanceEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val title = if (formState?.isEditing == true) "Edit Controlled Substance" else "Add Controlled Substance"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        ControlledSubstanceEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ControlledSubstanceEditForm(
    viewModel: ControlledSubstanceEditViewModel,
    formState: ControlledSubstanceFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            ControlledSubstanceBasicFields(viewModel, form)
            ControlledSubstanceAdministrationFields(viewModel, form)
            ControlledSubstanceMetaFields(viewModel, form)
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
private fun ControlledSubstanceBasicFields(
    viewModel: ControlledSubstanceEditViewModel,
    form: ControlledSubstanceFormState,
) {
    OutlinedTextField(
        value = form.drugName,
        onValueChange = viewModel::onDrugNameChange,
        label = { Text("Drug Name *") },
        isError = form.drugNameError != null,
        supportingText = { form.drugNameError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.dose,
        onValueChange = viewModel::onDoseChange,
        label = { Text("Dose *") },
        isError = form.doseError != null,
        supportingText = { form.doseError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.unit.orEmpty(),
        onValueChange = viewModel::onUnitChange,
        label = { Text("Unit") },
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
}

@Composable
private fun ControlledSubstanceAdministrationFields(
    viewModel: ControlledSubstanceEditViewModel,
    form: ControlledSubstanceFormState,
) {
    OutlinedTextField(
        value = form.administeredBy.orEmpty(),
        onValueChange = viewModel::onAdministeredByChange,
        label = { Text("Administered By") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.witness.orEmpty(),
        onValueChange = viewModel::onWitnessChange,
        label = { Text("Witness") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.date,
        onValueChange = viewModel::onDateChange,
        label = { Text("Date (yyyy-MM-dd) *") },
        isError = form.dateError != null,
        supportingText = { form.dateError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ControlledSubstanceMetaFields(
    viewModel: ControlledSubstanceEditViewModel,
    form: ControlledSubstanceFormState,
) {
    OutlinedTextField(
        value = form.reason.orEmpty(),
        onValueChange = viewModel::onReasonChange,
        label = { Text("Reason") },
        minLines = 2,
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
