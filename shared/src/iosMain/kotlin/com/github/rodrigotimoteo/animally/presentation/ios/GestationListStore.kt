@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListUiState
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [GestationListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("GestationListStore")
class GestationListStore(
    private val viewModel: GestationListViewModel,
) {
    /** Observable state of the gestation list screen. */
    val state: NativeFlow<GestationListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the gestation records for the patient. */
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
