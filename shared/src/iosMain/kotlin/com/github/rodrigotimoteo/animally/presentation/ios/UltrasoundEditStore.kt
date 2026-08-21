@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundEditViewModel
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the ultrasound add/edit form.
 *
 * Wraps the view model's nullable [UltrasoundFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("UltrasoundEditStoreState")
data class UltrasoundEditStoreState(
    val form: UltrasoundFormState? = null,
)

/**
 * Swift-facing store wrapping [UltrasoundEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("UltrasoundEditStore")
class UltrasoundEditStore(
    private val viewModel: UltrasoundEditViewModel,
) {
    /** Observable form state of the ultrasound add/edit screen. */
    val state: NativeFlow<UltrasoundEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { UltrasoundEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = UltrasoundEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the ultrasound date. */
    fun onDateChange(date: String) {
        viewModel.onDateChange(date)
    }

    /** Updates the ovary status. */
    fun onOvaryStatusChange(value: String) {
        viewModel.onOvaryStatusChange(value)
    }

    /** Updates the uterine status. */
    fun onUterineStatusChange(value: String) {
        viewModel.onUterineStatusChange(value)
    }

    /** Updates the follicle size in millimeters. */
    fun onFollicleSizeMmChange(value: String) {
        viewModel.onFollicleSizeMmChange(value)
    }

    /** Updates the findings. */
    fun onFindingsChange(value: String) {
        viewModel.onFindingsChange(value)
    }

    /** Updates the comma-separated attached image paths. */
    fun onImageUrisChange(value: String) {
        viewModel.onImageUrisChange(value)
    }

    /**
     * Saves the picked [files] to storage and appends their paths to the
     * attached images.
     */
    fun onFilesPicked(files: List<PickedFile>) {
        viewModel.onFilesPicked(files)
    }

    /** Removes [uri] from the attached images. */
    fun removeImageUri(uri: String) {
        viewModel.removeImageUri(uri)
    }

    /** Updates the name of the attending veterinarian. */
    fun onVetNameChange(value: String) {
        viewModel.onVetNameChange(value)
    }

    /** Updates the free-form notes. */
    fun onNotesChange(value: String) {
        viewModel.onNotesChange(value)
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
