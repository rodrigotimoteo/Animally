@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListUiState
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [LamenessListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("LamenessListStore")
class LamenessListStore(
    private val viewModel: LamenessListViewModel,
) {
    /** Observable state of the lameness list screen. */
    val state: NativeFlow<LamenessListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the lameness records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
