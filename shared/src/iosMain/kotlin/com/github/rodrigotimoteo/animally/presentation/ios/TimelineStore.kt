@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineUiState
import com.github.rodrigotimoteo.animally.presentation.timeline.TimelineViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [TimelineViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("TimelineStore")
class TimelineStore(
    private val viewModel: TimelineViewModel,
) {
    /** Observable state of the timeline screen. */
    val state: NativeFlow<TimelineUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the timeline feed. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
