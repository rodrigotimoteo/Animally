@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ios.base.GenericEditStore
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ownerEdit.OwnerFormState
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
 * Delegates NativeFlow binding and save/dismiss to [GenericEditStore].
 */
@ObjCName("OwnerEditStore")
class OwnerEditStore(
    viewModel: OwnerEditViewModel,
) : GenericEditStore<OwnerEditViewModel, OwnerFormState, OwnerEditStoreState>(viewModel) {
    /** Observable form state of the owner add/edit screen. */
    override val state: NativeFlow<OwnerEditStoreState> =
        viewModel.formState.toStoreStateFlow(OwnerEditStoreState()) { OwnerEditStoreState(form = it) }

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

    /** Updates the owner's map location from the iOS map picker. */
    fun onLocationChange(
        latitude: Double,
        longitude: Double,
    ) {
        viewModel.onLocationChange(latitude, longitude)
    }

    /** Removes the owner's optional map location. */
    fun clearLocation() {
        viewModel.clearLocation()
    }
}
