@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.coggins.CogginsUiState
import com.github.rodrigotimoteo.animally.presentation.coggins.CogginsViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [CogginsViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("CogginsStore")
class CogginsStore(
    private val viewModel: CogginsViewModel,
) {
    /** Observable state of the Coggins alerts section. */
    val state: NativeFlow<CogginsUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the Coggins alerts and hands them to the platform scheduler. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
