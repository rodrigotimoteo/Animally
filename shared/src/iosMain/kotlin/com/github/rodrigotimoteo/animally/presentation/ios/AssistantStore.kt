@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.llm.LlmAvailability
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantUiState
import com.github.rodrigotimoteo.animally.presentation.assistant.AssistantViewModel
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing store wrapping [AssistantViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("AssistantStore")
class AssistantStore(
    private val viewModel: AssistantViewModel,
) {
    /** Observable state of the AI assistant screen. */
    val state: NativeFlow<AssistantUiState> =
        NativeFlow(viewModel.uiState, viewModel.viewModelScope)

    /** Asks the assistant a free-text question about the records. */
    fun ask(question: String) {
        viewModel.ask(question)
    }

    /** Re-checks platform LLM availability. */
    fun refreshAvailability() {
        viewModel.refreshAvailability()
    }

    /** Clears the current error message. */
    fun dismissError() {
        viewModel.dismissError()
    }

    /** Convenience mirror of the availability value for one-shot checks. */
    val availability: LlmAvailability
        get() = viewModel.uiState.value.availability
}
