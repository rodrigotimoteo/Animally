@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [EmbryoTransferListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("EmbryoTransferListStore")
class EmbryoTransferListStore(
    private val viewModel: EmbryoTransferListViewModel,
) {
    /** Observable state of the embryo transfer list screen. */
    val state: NativeFlow<com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferListUiState> =
        NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the embryo transfer records for the patient. */
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
