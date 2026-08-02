package com.github.rodrigotimoteo.animally.presentation.consultation.view

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
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationEditViewModel
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationFormState

/**
 * Screen for creating or editing a consultation documented via SOAP notes.
 *
 * @param viewModel The [ConsultationEditViewModel] for this screen.
 * @param modifier Optional modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationEditScreen(
    viewModel: ConsultationEditViewModel,
    modifier: Modifier = Modifier,
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState?.isEditing == true) "Edit Consultation" else "Add Consultation") },
                navigationIcon = {
                    TextButton(onClick = viewModel::onBack) {
                        Text("Cancel")
                    }
                },
            )
        },
    ) { innerPadding ->
        ConsultationEditForm(
            viewModel = viewModel,
            formState = formState,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ConsultationEditForm(
    viewModel: ConsultationEditViewModel,
    formState: ConsultationFormState?,
    modifier: Modifier,
) {
    val columnModifier = modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())
    Column(modifier = columnModifier) {
        if (formState?.isLoading == true) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }
        formState?.let { form ->
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
                value = form.vetName.orEmpty(),
                onValueChange = viewModel::onVetNameChange,
                label = { Text("Vet Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SoapField(viewModel, form, ConsultationField.SUBJECTIVE)
            SoapField(viewModel, form, ConsultationField.OBJECTIVE)
            SoapField(viewModel, form, ConsultationField.ASSESSMENT)
            SoapField(viewModel, form, ConsultationField.PLAN)
            OutlinedTextField(
                value = form.nextVisitDate.orEmpty(),
                onValueChange = viewModel::onNextVisitDateChange,
                label = { Text("Next Visit Date (yyyy-MM-dd)") },
                isError = form.nextVisitDateError != null,
                supportingText = { form.nextVisitDateError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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

private enum class ConsultationField(
    val label: String,
) {
    SUBJECTIVE("Subjective"),
    OBJECTIVE("Objective"),
    ASSESSMENT("Assessment"),
    PLAN("Plan"),
}

@Composable
private fun SoapField(
    viewModel: ConsultationEditViewModel,
    form: ConsultationFormState,
    field: ConsultationField,
) {
    val value =
        when (field) {
            ConsultationField.SUBJECTIVE -> form.subjective
            ConsultationField.OBJECTIVE -> form.objective
            ConsultationField.ASSESSMENT -> form.assessment
            ConsultationField.PLAN -> form.plan
        }
    val onValueChange =
        when (field) {
            ConsultationField.SUBJECTIVE -> viewModel::onSubjectiveChange
            ConsultationField.OBJECTIVE -> viewModel::onObjectiveChange
            ConsultationField.ASSESSMENT -> viewModel::onAssessmentChange
            ConsultationField.PLAN -> viewModel::onPlanChange
        }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}
