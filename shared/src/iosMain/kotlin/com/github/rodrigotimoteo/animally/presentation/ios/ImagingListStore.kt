@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingListUiState
import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ImagingListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ImagingListStore")
class ImagingListStore(
    private val viewModel: ImagingListViewModel,
) {
    /** Observable state of the imaging list screen. */
    val state: NativeFlow<ImagingListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the imaging records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Soft-deletes the record with the given [recordId] and reloads the list. */
    fun delete(recordId: Long) {
        viewModel.onDeleteClick(recordId)
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
