@file:OptIn(ExperimentalObjCName::class)

package com.github.rodrigotimoteo.animally.presentation.ios

import androidx.lifecycle.viewModelScope
import com.github.rodrigotimoteo.animally.bridge.NativeFlow
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderEditViewModel
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderFormState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Swift-facing state of the custom reminder add/edit form.
 *
 * Wraps the view model's nullable [CustomReminderFormState], so the store
 * exposes a non-null [NativeFlow] value.
 *
 * @property form The current form state, or `null` before the form has loaded.
 */
@ObjCName("CustomReminderEditStoreState")
data class CustomReminderEditStoreState(
    val form: CustomReminderFormState? = null,
)

/**
 * Swift-facing store wrapping [CustomReminderEditViewModel].
 *
 * Exposes only data actions; navigation is owned by SwiftUI.
 */
@ObjCName("CustomReminderEditStore")
class CustomReminderEditStore(
    private val viewModel: CustomReminderEditViewModel,
) {
    /** Observable form state of the custom reminder add/edit screen. */
    val state: NativeFlow<CustomReminderEditStoreState> =
        NativeFlow(
            viewModel.formState
                .map { CustomReminderEditStoreState(form = it) }
                .stateIn(
                    scope = viewModel.viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = CustomReminderEditStoreState(form = null),
                ),
            viewModel.viewModelScope,
        )

    /** Updates the reminder title. */
    fun onTitleChange(value: String) {
        viewModel.onTitleChange(value)
    }

    /** Updates the due date. */
    fun onDueDateChange(value: String) {
        viewModel.onDueDateChange(value)
    }

    /** Updates the type of the linked record, if any. */
    fun onLinkedRecordTypeChange(value: String) {
        viewModel.onLinkedRecordTypeChange(value)
    }

    /** Updates the id of the linked record, if any. */
    fun onLinkedRecordIdChange(value: String) {
        viewModel.onLinkedRecordIdChange(value)
    }

    /** Updates the free-form notes. */
    fun onNotesChange(value: String) {
        viewModel.onNotesChange(value)
    }

    /** Validates and persists the current form. */
    fun save() {
        viewModel.save()
    }

    /** Dismisses the error surfaced by the form, if any. */
    fun dismissError() {
        viewModel.onDismissError()
    }
}
