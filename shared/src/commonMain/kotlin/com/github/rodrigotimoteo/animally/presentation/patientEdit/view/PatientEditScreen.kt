package com.github.rodrigotimoteo.animally.presentation.patientEdit.view

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.presentation.patientEdit.CogginsField
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientEditViewModel
import com.github.rodrigotimoteo.animally.presentation.patientEdit.PatientFormState

/**
 * Screen for creating or editing a patient.
 *
 * @param viewModel The [PatientEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditScreen(
    viewModel: PatientEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val owners by viewModel.owners.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Patient" else "Add Patient") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        PatientEditForm(
            viewModel = viewModel,
            formState = formState,
            owners = owners,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun PatientEditForm(
    viewModel: PatientEditViewModel,
    formState: PatientFormState?,
    owners: List<Owner>,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            PatientIdentityFields(viewModel, form)
            PatientIdFields(viewModel, form)
            PatientDetailFields(viewModel, form)
            CogginsFields(viewModel, form)
            OwnerSelector(
                owners = owners,
                selectedOwnerId = form.ownerId,
                onOwnerChange = viewModel::onOwnerChange,
            )
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
private fun PatientIdentityFields(
    viewModel: PatientEditViewModel,
    form: PatientFormState,
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
        value = form.species,
        onValueChange = viewModel::onSpeciesChange,
        label = { Text("Species") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.breed.orEmpty(),
        onValueChange = viewModel::onBreedChange,
        label = { Text("Breed") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.gender.orEmpty(),
        onValueChange = viewModel::onGenderChange,
        label = { Text("Gender") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PatientIdFields(
    viewModel: PatientEditViewModel,
    form: PatientFormState,
) {
    OutlinedTextField(
        value = form.microchipId.orEmpty(),
        onValueChange = viewModel::onMicrochipIdChange,
        label = { Text("Microchip ID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.ueln.orEmpty(),
        onValueChange = viewModel::onUelnChange,
        label = { Text("UELN") },
        isError = form.uelnError != null,
        supportingText = { form.uelnError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.registrationNumber.orEmpty(),
        onValueChange = viewModel::onRegistrationNumberChange,
        label = { Text("Registration Number") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PatientDetailFields(
    viewModel: PatientEditViewModel,
    form: PatientFormState,
) {
    OutlinedTextField(
        value = form.dateOfBirth.orEmpty(),
        onValueChange = viewModel::onDateOfBirthChange,
        label = { Text("Date of Birth (yyyy-MM-dd)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.stableLocation.orEmpty(),
        onValueChange = viewModel::onStableLocationChange,
        label = { Text("Stable Location") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.photoUri.orEmpty(),
        onValueChange = viewModel::onPhotoUriChange,
        label = { Text("Photo URI") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.notes.orEmpty(),
        onValueChange = viewModel::onNotesChange,
        label = { Text("Notes") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CogginsFields(
    viewModel: PatientEditViewModel,
    form: PatientFormState,
) {
    OutlinedTextField(
        value = form.cogginsTestDate.orEmpty(),
        onValueChange = { viewModel.onCogginsChange(CogginsField.TEST_DATE, it) },
        label = { Text("Coggins Test Date (yyyy-MM-dd)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.cogginsResult.orEmpty(),
        onValueChange = { viewModel.onCogginsChange(CogginsField.RESULT, it) },
        label = { Text("Coggins Result") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.cogginsExpiryDate.orEmpty(),
        onValueChange = { viewModel.onCogginsChange(CogginsField.EXPIRY_DATE, it) },
        label = { Text("Coggins Expiry Date (yyyy-MM-dd)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OwnerSelector(
    owners: List<Owner>,
    selectedOwnerId: Long?,
    onOwnerChange: (Long?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Owner", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = selectedOwnerId == null,
                onClick = { onOwnerChange(null) },
                label = { Text("None") },
            )
            owners.forEach { owner ->
                FilterChip(
                    selected = selectedOwnerId == owner.id,
                    onClick = { onOwnerChange(owner.id) },
                    label = { Text(owner.name) },
                )
            }
        }
    }
}
