package com.github.rodrigotimoteo.animally.presentation.surgery.view

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
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryEditViewModel
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryFormState

/**
 * Screen for creating or editing a surgery.
 *
 * @param viewModel The [SurgeryEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurgeryEditScreen(
    viewModel: SurgeryEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Surgery" else "Add Surgery") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        SurgeryEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SurgeryEditForm(
    viewModel: SurgeryEditViewModel,
    formState: SurgeryFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            SurgeryBasicFields(viewModel, form)
            SurgeryTeamFields(viewModel, form)
            SurgeryRecoveryFields(viewModel, form)
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
private fun SurgeryBasicFields(
    viewModel: SurgeryEditViewModel,
    form: SurgeryFormState,
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
        value = form.type.orEmpty(),
        onValueChange = viewModel::onTypeChange,
        label = { Text("Type") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.description.orEmpty(),
        onValueChange = viewModel::onDescriptionChange,
        label = { Text("Description") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SurgeryTeamFields(
    viewModel: SurgeryEditViewModel,
    form: SurgeryFormState,
) {
    OutlinedTextField(
        value = form.outcome.orEmpty(),
        onValueChange = viewModel::onOutcomeChange,
        label = { Text("Outcome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.surgeon.orEmpty(),
        onValueChange = viewModel::onSurgeonChange,
        label = { Text("Surgeon") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.anesthesia.orEmpty(),
        onValueChange = viewModel::onAnesthesiaChange,
        label = { Text("Anesthesia") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SurgeryRecoveryFields(
    viewModel: SurgeryEditViewModel,
    form: SurgeryFormState,
) {
    OutlinedTextField(
        value = form.analgesia.orEmpty(),
        onValueChange = viewModel::onAnalgesiaChange,
        label = { Text("Analgesia") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.complications.orEmpty(),
        onValueChange = viewModel::onComplicationsChange,
        label = { Text("Complications") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.recoveryNotes.orEmpty(),
        onValueChange = viewModel::onRecoveryNotesChange,
        label = { Text("Recovery Notes") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
}
