@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.care.UpcomingCareUiState
import com.github.rodrigotimoteo.animally.presentation.care.UpcomingCareViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [UpcomingCareViewModel] for the Care Due panel
 * on the patient Overview tab.
 *
 * Exposes only data; navigation is owned by SwiftUI.
 */
@ObjCName("UpcomingCareStore")
class UpcomingCareStore(
    private val viewModel: UpcomingCareViewModel,
) {
    /** Observable state of the Care Due panel. */
    val state: NativeFlow<UpcomingCareUiState> = NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Reloads the care-due list for the patient. */
    fun load() {
        viewModel.load()
    }
}
