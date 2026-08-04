@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationListUiState
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [MedicationListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("MedicationListStore")
class MedicationListStore(
    private val viewModel: MedicationListViewModel,
) {
    /** Observable state of the medication list screen. */
    val state: NativeFlow<MedicationListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the medication records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
