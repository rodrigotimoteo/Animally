@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListUiState
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [UltrasoundListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("UltrasoundListStore")
class UltrasoundListStore(
    private val viewModel: UltrasoundListViewModel,
) {
    /** Observable state of the ultrasound list screen. */
    val state: NativeFlow<UltrasoundListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the ultrasound records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
