@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailUiState
import com.github.rodrigotimoteo.animally.presentation.patientDetail.PatientDetailViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [PatientDetailViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("PatientDetailStore")
class PatientDetailStore(
    private val viewModel: PatientDetailViewModel,
) {
    /** Observable state of the patient detail screen. */
    val state: NativeFlow<PatientDetailUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the patient and its linked owner. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
