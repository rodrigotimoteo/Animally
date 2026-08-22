@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListUiState
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [DewormingListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("DewormingListStore")
class DewormingListStore(
    private val viewModel: DewormingListViewModel,
) {
    /** Observable state of the deworming list screen. */
    val state: NativeFlow<DewormingListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the deworming records for the patient. */
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
