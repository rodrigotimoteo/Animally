@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListUiState
import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [ConsultationListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("ConsultationListStore")
class ConsultationListStore(
    private val viewModel: ConsultationListViewModel,
) {
    /** Observable state of the consultation list screen. */
    val state: NativeFlow<ConsultationListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the consultation records for the patient. */
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
