@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsRecordsUiState
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsRecordsViewModel
import kotlinx.coroutines.cancel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [InsightsRecordsViewModel].
 *
 * Delegates only to the shared ViewModel; contains no calculation,
 * date arithmetic, or database access.
 * Exposes observable state via [NativeFlow] and forwards user actions.
 */
@ObjCName("InsightsRecordsStore")
class InsightsRecordsStore(
    private val viewModel: InsightsRecordsViewModel,
) {
    /** Observable state of the filtered record list. */
    val state: NativeFlow<InsightsRecordsUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads using the current drill-down filter. */
    fun reload() {
        viewModel.reload()
    }

    /** Clears the last load error message. */
    fun dismissError() {
        viewModel.dismissError()
    }

    /** Cancels the ViewModel scope — called from Swift deinit to prevent Koin leak. */
    fun clear() {
        viewModel.viewModelScope.cancel()
    }
}
