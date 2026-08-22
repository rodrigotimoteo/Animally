@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferEditViewModel
import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the embryo transfer add/edit form.
 *
 * Wraps the view model's nullable [EmbryoTransferFormState], so the store
 * exposes a non-null [NativeFlow] value.
 */
data class EmbryoTransferEditStoreState(
    val form: EmbryoTransferFormState?,
)

/**
 * Swift-facing store wrapping [EmbryoTransferEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("EmbryoTransferEditStore")
class EmbryoTransferEditStore(
    private val viewModel: EmbryoTransferEditViewModel,
) {
    /** Observable form state of the embryo transfer add/edit screen. */
    val state: NativeFlow<EmbryoTransferEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { EmbryoTransferEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = EmbryoTransferEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the procedure date. */
    fun onDateChange(value: String) {
        viewModel.onDateChange(value)
    }

    /** Updates the number of collected embryos. */
    fun onEmbryoCountChange(value: String) {
        viewModel.onEmbryoCountChange(value)
    }

    /** Updates the recipient mares. */
    fun onRecipientMaresChange(value: String) {
        viewModel.onRecipientMaresChange(value)
    }

    /** Updates the attending veterinarian. */
    fun onVetNameChange(value: String) {
        viewModel.onVetNameChange(value)
    }

    /** Updates the notes. */
    fun onNotesChange(value: String) {
        viewModel.onNotesChange(value)
    }

    /** Validates and persists the form. */
    fun save() {
        viewModel.save()
    }
}
