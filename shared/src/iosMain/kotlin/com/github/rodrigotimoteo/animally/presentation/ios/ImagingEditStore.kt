@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.data.storage.PickedFile
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingEditViewModel
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the imaging add/edit form.
 *
 * Wraps the view model's nullable [ImagingFormState], so the store exposes a
 * non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("ImagingEditStoreState")
data class ImagingEditStoreState(
    val form: ImagingFormState? = null,
)

/**
 * Swift-facing store wrapping [ImagingEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ImagingEditStore")
class ImagingEditStore(
    private val viewModel: ImagingEditViewModel,
) {
    /** Observable form state of the imaging add/edit screen. */
    val state: NativeFlow<ImagingEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { ImagingEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ImagingEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the imaging type (radiograph, ultrasound, CT, MRI). */
    fun onTypeChange(value: String) {
        viewModel.onTypeChange(value)
    }

    /** Updates the imaging date. */
    fun onDateChange(value: String) {
        viewModel.onDateChange(value)
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
