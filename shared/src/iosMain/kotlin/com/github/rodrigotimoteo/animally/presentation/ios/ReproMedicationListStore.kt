@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListUiState
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ReproMedicationListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ReproMedicationListStore")
class ReproMedicationListStore(
    private val viewModel: ReproMedicationListViewModel,
) {
    /** Observable state of the repromedication list screen. */
    val state: NativeFlow<ReproMedicationListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the repromedication records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
