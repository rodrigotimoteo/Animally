@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryListUiState
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [SurgeryListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SurgeryListStore")
class SurgeryListStore(
    private val viewModel: SurgeryListViewModel,
) {
    /** Observable state of the surgery list screen. */
    val state: NativeFlow<SurgeryListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the surgery records for the patient. */
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
