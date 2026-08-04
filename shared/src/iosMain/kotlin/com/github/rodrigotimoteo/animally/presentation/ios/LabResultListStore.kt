@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultListUiState
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [LabResultListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("LabResultListStore")
class LabResultListStore(
    private val viewModel: LabResultListViewModel,
) {
    /** Observable state of the labresult list screen. */
    val state: NativeFlow<LabResultListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the labresult records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
