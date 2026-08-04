@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailUiState
import com.github.rodrigotimoteo.animally.presentation.ownerDetail.OwnerDetailViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [OwnerDetailViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("OwnerDetailStore")
class OwnerDetailStore(
    private val viewModel: OwnerDetailViewModel,
) {
    /** Observable state of the owner detail screen. */
    val state: NativeFlow<OwnerDetailUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the owner and its linked patients. */
    fun load() {
        viewModel.load()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
