package com.github.rodrigotimoteo.animally.presentation.reproduction.view

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventEditViewModel
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventFormState

private val EVENT_TYPES = listOf("Heat", "Breeding", "PregnancyCheck", "Foaling")

/**
 * Screen for creating or editing a reproduction-cycle event.
 *
 * @param viewModel The [ReproductionEventEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReproductionEventEditScreen(
    viewModel: ReproductionEventEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Event" else "Add Event") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        ReproductionEventEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ReproductionEventEditForm(
    viewModel: ReproductionEventEditViewModel,
    formState: ReproductionEventFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
            ReproductionEventTypeFields(viewModel, form)
            ReproductionEventDetailsFields(viewModel, form)
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
private fun ReproductionEventTypeFields(
    viewModel: ReproductionEventEditViewModel,
    form: ReproductionEventFormState,
) {
    Text("Event Type *", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EVENT_TYPES.forEach { type ->
            FilterChip(
                selected = form.eventType == type,
                onClick = { viewModel.onEventTypeChange(type) },
                label = { Text(type) },
            )
        }
    }
    form.eventTypeError?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun ReproductionEventDetailsFields(
    viewModel: ReproductionEventEditViewModel,
    form: ReproductionEventFormState,
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
        value = form.details.orEmpty(),
        onValueChange = viewModel::onDetailsChange,
        label = { Text("Details") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
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
