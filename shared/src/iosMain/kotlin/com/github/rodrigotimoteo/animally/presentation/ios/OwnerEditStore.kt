@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the owner add/edit form.
 *
 * Wraps the view model's nullable [OwnerFormState] so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("OwnerEditStoreState")
data class OwnerEditStoreState(
    val form: OwnerFormState? = null,
)

/**
 * Swift-facing store wrapping [OwnerEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("OwnerEditStore")
class OwnerEditStore(
    private val viewModel: OwnerEditViewModel,
) {
    /** Observable form state of the owner add/edit screen. */
    val state: NativeFlow<OwnerEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { OwnerEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = OwnerEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the owner's name. */
    fun onNameChange(name: String) {
        viewModel.onNameChange(name)
    }

    /** Updates the owner's phone number. */
    fun onPhoneChange(phone: String) {
        viewModel.onPhoneChange(phone)
    }

    /** Updates the owner's email address. */
    fun onEmailChange(email: String) {
        viewModel.onEmailChange(email)
    }

    /** Updates the owner's physical address. */
    fun onAddressChange(address: String) {
        viewModel.onAddressChange(address)
    }

    /** Validates and persists the current form. */
    fun save() {
        viewModel.save()
    }

    /** Dismisses the error surfaced by the form, if any. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
