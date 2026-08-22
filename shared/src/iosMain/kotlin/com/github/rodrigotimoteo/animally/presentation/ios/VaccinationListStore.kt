@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListUiState
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [VaccinationListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("VaccinationListStore")
class VaccinationListStore(
    private val viewModel: VaccinationListViewModel,
) {
    /** Observable state of the vaccination list screen. */
    val state: NativeFlow<VaccinationListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the vaccination records for the patient. */
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
