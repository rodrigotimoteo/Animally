@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.search.SearchUiState
import com.github.rodrigotimoteo.animally.presentation.search.SearchViewModel
import kotlinx.datetime.LocalDate
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [SearchViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("SearchStore")
class SearchStore(
    private val viewModel: SearchViewModel,
) {
    /** Observable state of the global search screen. */
    val state: NativeFlow<SearchUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Updates the query text and re-runs the search. */
    fun setQuery(query: String) {
        viewModel.onQueryChange(query)
    }

    /** Toggles the given [recordType] filter chip. */
    fun toggleRecordType(recordType: String) {
        viewModel.toggleRecordType(recordType)
    }

    /** Updates the optional lower bound for the record date. */
    fun setFromDate(date: LocalDate?) {
        viewModel.setFromDate(date)
    }

    /** Updates the optional upper bound for the record date. */
    fun setToDate(date: LocalDate?) {
        viewModel.setToDate(date)
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
