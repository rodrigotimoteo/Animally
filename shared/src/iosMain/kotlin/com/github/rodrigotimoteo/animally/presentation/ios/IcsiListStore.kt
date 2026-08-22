@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [IcsiListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("IcsiListStore")
class IcsiListStore(
    private val viewModel: IcsiListViewModel,
) {
    /** Observable state of the ICSI list screen. */
    val state: NativeFlow<com.github.rodrigotimoteo.animally.presentation.icsi.IcsiListUiState> =
        NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the ICSI records for the patient. */
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
