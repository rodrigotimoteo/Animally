package com.github.rodrigotimoteo.animally.presentation.ios.base

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.common.addEdit.BaseAddEditViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Generic Swift-facing edit-store base.
 *
 * Eliminates ~12 LOC copy-paste per `*EditStore` (NativeFlow binding + save/dismiss).
 * Concrete stores keep their `@ObjCName` and per-field `onXChange` methods; only
 * the shared `state` wiring and `save`/`dismissError` delegation are centralized.
 *
 * @param Vm concrete edit ViewModel type, must extend [BaseAddEditViewModel]
 * @param FormState form state type held by [Vm.formState]
 * @param StoreState Swift-facing wrapper (e.g. `WeightEditStoreState(form: WeightFormState?)`)
 */
abstract class GenericEditStore<Vm : BaseAddEditViewModel<FormState>, FormState : Any, StoreState : Any>(
    protected val viewModel: Vm,
) {
    /**
     * Swift-observable state. Subclasses provide the [StateFlow] that backs it.
     * Use [Flow.toStoreStateFlow] for the common single-form case, or build a
     * combined flow (e.g. `combine(formState, owners)`) for stores like [PatientEditStore].
     */
    abstract val state: NativeFlow<StoreState>

    /** Validates and persists the current form. */
    fun save() {
        viewModel.save()
    }

    /** Dismisses the error surfaced by the form, if any. */
    fun dismissError() {
        viewModel.onDismissError()
    }

    /**
     * Helper for the common case: map nullable [FormState] to [StoreState].
     *
     * ```
     * override val state: NativeFlow<WeightEditStoreState> =
     *   viewModel.formState.toStoreStateFlow(WeightEditStoreState()) { WeightEditStoreState(form = it) }
     * ```
     */
    protected fun Flow<FormState?>.toStoreStateFlow(
        initial: StoreState,
        mapper: (FormState?) -> StoreState,
    ): NativeFlow<StoreState> {
        val flow: StateFlow<StoreState> =
            map(mapper).stateIn(viewModel.viewModelScope, SharingStarted.Eagerly, initial)
        return NativeFlow(flow, viewModel.viewModelScope)
    }

    /**
     * Helper for pre-built [StateFlow] (e.g. combined flows).
     */
    protected fun StateFlow<StoreState>.toNativeFlow(): NativeFlow<StoreState> = NativeFlow(this, viewModel.viewModelScope)
}
