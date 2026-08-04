@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListUiState
import com.github.rodrigotimoteo.animally.presentation.ownerList.OwnerListViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [OwnerListViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("OwnerListStore")
class OwnerListStore(
    private val viewModel: OwnerListViewModel,
) {
    /** Observable state of the owner list. */
    val state: NativeFlow<OwnerListUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the owner list. */
    fun load() {
        viewModel.loadOwners()
    }

    /** Soft-deletes the owner with the given [ownerId]. */
    fun deleteOwner(ownerId: Long) {
        viewModel.onDeleteClick(ownerId)
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
