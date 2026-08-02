package com.github.rodrigotimoteo.animally.presentation.ultrasound.view

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
import com.github.rodrigotimoteo.animally.presentation.common.attachment.AttachmentImagePicker
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundFormState

/**
 * Screen for creating or editing a reproductive ultrasound.
 *
 * @param viewModel The [UltrasoundEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltrasoundEditScreen(
    viewModel: UltrasoundEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Ultrasound" else "Add Ultrasound") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        UltrasoundEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun UltrasoundEditForm(
    viewModel: UltrasoundEditViewModel,
    formState: UltrasoundFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            UltrasoundBasicFields(viewModel, form)
            UltrasoundFindingsFields(viewModel, form)
            UltrasoundMetaFields(viewModel, form)
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
private fun UltrasoundBasicFields(
    viewModel: UltrasoundEditViewModel,
    form: UltrasoundFormState,
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
        value = form.ovaryStatus.orEmpty(),
        onValueChange = viewModel::onOvaryStatusChange,
        label = { Text("Ovary Status") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.uterineStatus.orEmpty(),
        onValueChange = viewModel::onUterineStatusChange,
        label = { Text("Uterine Status") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.follicleSizeMm.orEmpty(),
        onValueChange = viewModel::onFollicleSizeMmChange,
        label = { Text("Follicle Size (mm)") },
        isError = form.follicleSizeMmError != null,
        supportingText = { form.follicleSizeMmError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UltrasoundFindingsFields(
    viewModel: UltrasoundEditViewModel,
    form: UltrasoundFormState,
) {
    OutlinedTextField(
        value = form.findings.orEmpty(),
        onValueChange = viewModel::onFindingsChange,
        label = { Text("Findings") },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
    AttachmentImagePicker(
        imageUris = form.imageUris,
        onFilesPicked = viewModel::onFilesPicked,
        onRemove = viewModel::removeImageUri,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun UltrasoundMetaFields(
    viewModel: UltrasoundEditViewModel,
    form: UltrasoundFormState,
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
