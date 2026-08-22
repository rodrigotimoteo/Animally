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
@Suppress("TooManyFunctions")
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

    /** Updates the left ovary status. */
    fun onLeftOvaryStatusChange(value: String) {
        viewModel.onLeftOvaryStatusChange(value)
    }

    /** Updates the right ovary status. */
    fun onRightOvaryStatusChange(value: String) {
        viewModel.onRightOvaryStatusChange(value)
    }

    /** Updates the left ovary follicle size in millimeters. */
    fun onLeftFollicleSizeMmChange(value: String) {
        viewModel.onLeftFollicleSizeMmChange(value)
    }

    /** Updates the right ovary follicle size in millimeters. */
    fun onRightFollicleSizeMmChange(value: String) {
        viewModel.onRightFollicleSizeMmChange(value)
    }

    /** Updates the uterine edema description. */
    fun onUterineEdemaChange(value: String) {
        viewModel.onUterineEdemaChange(value)
    }

    /** Updates whether fluid is present in the uterus. */
    fun onUterineLiquidChange(value: Boolean?) {
        viewModel.onUterineLiquidChange(value)
    }

    /** Updates the uterine fluid description. */
    fun onUterineLiquidDescriptionChange(value: String) {
        viewModel.onUterineLiquidDescriptionChange(value)
    }

    /** Updates the general uterus description. */
    fun onUterusDescriptionChange(value: String) {
        viewModel.onUterusDescriptionChange(value)
    }

    /** Adds an empty follicle row on the given ovary side. */
    fun onAddFollicle(side: String) {
        viewModel.onAddFollicle(side)
    }

    /** Removes the follicle row at index on the given ovary side. */
    fun onRemoveFollicle(
        side: String,
        index: Long,
    ) {
        viewModel.onRemoveFollicle(side, index.toInt())
    }

    /** Updates the size of the follicle row at index on the given ovary side. */
    fun onFollicleSizeChange(
        side: String,
        index: Long,
        value: String,
    ) {
        viewModel.onFollicleSizeChange(side, index.toInt(), value)
    }

    /** Updates the description of the follicle row at index on the given ovary side. */
    fun onFollicleDescriptionChange(
        side: String,
        index: Long,
        value: String,
    ) {
        viewModel.onFollicleDescriptionChange(side, index.toInt(), value)
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
