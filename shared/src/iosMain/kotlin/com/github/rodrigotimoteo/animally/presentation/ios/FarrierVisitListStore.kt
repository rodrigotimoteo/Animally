@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListUiState
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [FarrierVisitListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("FarrierVisitListStore")
class FarrierVisitListStore(
    private val viewModel: FarrierVisitListViewModel,
) {
    /** Observable state of the farrier list screen. */
    val state: NativeFlow<FarrierVisitListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the farrier records for the patient. */
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
