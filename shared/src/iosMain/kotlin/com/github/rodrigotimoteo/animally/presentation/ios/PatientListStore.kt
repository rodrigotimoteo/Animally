@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListUiState
import com.github.rodrigotimoteo.animally.presentation.patientList.PatientListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [PatientListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("PatientListStore")
class PatientListStore(
    private val viewModel: PatientListViewModel,
) {
    /** Observable state of the patient list. */
    val state: NativeFlow<PatientListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the patient list. */
    fun load() {
        viewModel.loadPatients()
    }

    /** Soft-deletes the patient with the given [patientId]. */
    fun deletePatient(patientId: Long) {
        viewModel.onDeleteClick(patientId)
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
