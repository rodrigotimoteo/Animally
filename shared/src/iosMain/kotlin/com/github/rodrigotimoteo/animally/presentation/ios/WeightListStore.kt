@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListUiState
import com.github.rodrigotimoteo.animally.presentation.weight.WeightListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [WeightListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("WeightListStore")
class WeightListStore(
    private val viewModel: WeightListViewModel,
) {
    /** Observable state of the weight list screen. */
    val state: NativeFlow<WeightListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the weight records for the patient. */
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
