@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListUiState
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [UltrasoundListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("UltrasoundListStore")
class UltrasoundListStore(
    private val viewModel: UltrasoundListViewModel,
) {
    /** Observable state of the ultrasound list screen. */
    val state: NativeFlow<UltrasoundListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the ultrasound records for the patient. */
    fun load() {
        viewModel.load()
    }

    /** Soft-deletes the record with the given [recordId] and reloads the list. */
    fun delete(recordId: Long) {
        viewModel.onDeleteClick(recordId)
    }

    /** Opens the inline search field. */
    fun openSearch() {
        viewModel.onSearchClick()
    }

    /** Updates the inline search query. */
    fun updateSearch(query: String) {
        viewModel.onSearchQueryChange(query)
    }

    /** Closes the inline search field and clears its query. */
    fun closeSearch() {
        viewModel.onCloseSearch()
    }

    /** Toggles whether all matching records are shown. */
    fun toggleExpanded() {
        viewModel.onToggleExpanded()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
