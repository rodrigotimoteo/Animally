package com.github.rodrigotimoteo.animally.presentation.ios.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Generic Swift-facing list-store base.
 *
 * Centralizes the [NativeFlow] wrapping of a list [StateFlow<UiState>].
 * Concrete list stores keep their `@ObjCName`, typed `UiState`, and per-action
 * methods (`load`, `delete`, `openSearch`, …); only the observable `state`
 * plumbing is shared.
 *
 * @param UiState Swift-observable UI state type (e.g. [WeightListUiState])
 */
abstract class GenericListStore<UiState : Any>(
    protected val viewModel: ViewModel,
) {
    /**
     * Swift-observable state. Subclasses provide the backing [StateFlow].
     * Common implementation:
     * ```
     * override val state = viewModel.uiState.toNativeFlow()
     * ```
     */
    abstract val state: NativeFlow<UiState>

    /**
     * Wraps [StateFlow] in a [NativeFlow] tied to [viewModel.viewModelScope].
     */
    protected fun StateFlow<UiState>.toNativeFlow(): NativeFlow<UiState> = NativeFlow(this, viewModel.viewModelScope)
}
