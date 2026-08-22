@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListUiState
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ControlledSubstanceListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SubstanceListStore")
class SubstanceListStore(
    private val viewModel: ControlledSubstanceListViewModel,
) {
    /** Observable state of the substance list screen. */
    val state: NativeFlow<ControlledSubstanceListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the substance records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Soft-deletes the record with the given [recordId] and reloads the list. */
    fun delete(recordId: Long) {
        viewModel.onDeleteClick(recordId)
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
