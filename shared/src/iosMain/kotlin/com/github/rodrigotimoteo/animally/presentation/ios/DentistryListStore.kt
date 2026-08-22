@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryListUiState
import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [DentistryListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("DentistryListStore")
class DentistryListStore(
    private val viewModel: DentistryListViewModel,
) {
    /** Observable state of the dentistry list screen. */
    val state: NativeFlow<DentistryListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the dentistry records for the patient. */
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
