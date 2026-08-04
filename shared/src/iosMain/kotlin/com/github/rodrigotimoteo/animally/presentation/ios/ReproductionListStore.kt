@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventListUiState
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ReproductionEventListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ReproductionListStore")
class ReproductionListStore(
    private val viewModel: ReproductionEventListViewModel,
) {
    /** Observable state of the reproduction list screen. */
    val state: NativeFlow<ReproductionEventListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the reproduction records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
