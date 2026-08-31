@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsPreset
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsUiState
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsViewModel
import kotlinx.coroutines.cancel
import kotlinx.datetime.LocalDate
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [InsightsViewModel].
 *
 * Delegates only to the shared ViewModel; contains no calculation,
 * date arithmetic, formatting policy, or database access.
 * Exposes observable state via [NativeFlow] and forwards user actions.
 */
@ObjCName("InsightsStore")
class InsightsStore(
    private val viewModel: InsightsViewModel,
) {
    /** Observable state of the Insights dashboard. */
    val state: NativeFlow<InsightsUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads using the current preset, scope and custom inputs. */
    fun reload() {
        viewModel.reload()
    }

    /** Selects [preset] and triggers a reload. */
    fun selectPreset(preset: InsightsPreset) {
        viewModel.selectPreset(preset)
    }

    /**
     * Applies a custom inclusive range.
     *
     * Inputs stay visible when invalid; validation is set and no
     * repository call is made until the range is valid.
     */
    fun setCustomRange(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        viewModel.setCustomRange(from, to)
    }

    /** Updates patient scope and reloads. Null means all active patients. */
    fun setPatientScope(patientId: Long?) {
        viewModel.setPatientScope(patientId)
    }

    /** Clears the last load error message. */
    fun dismissError() {
        viewModel.dismissError()
    }

    /** Clears custom-range validation feedback. */
    fun dismissValidationError() {
        viewModel.dismissValidationError()
    }

    /** Cancels the ViewModel scope — called from Swift deinit to prevent Koin leak on rapid open/close. */
    fun clear() {
        viewModel.viewModelScope.cancel()
    }
}
