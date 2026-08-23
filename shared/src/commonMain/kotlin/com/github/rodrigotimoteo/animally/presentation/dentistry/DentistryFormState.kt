
package com.github.rodrigotimoteo.animally.presentation.dentistry

import com.github.rodrigotimoteo.animally.presentation.common.todayIso
import kotlin.time.Instant

/**
 * UI state for the dentistry add/edit form.
 *
 * @param id The persisted dentistry id; `null` when creating a new one.
 * @param date The dental check date as a display string (ISO `yyyy-MM-dd`).
 * @param findings Optional findings from the examination.
 * @param treatment Optional treatment performed.
 * @param nextDueDate Optional date the next dental check is due.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param nextDueDateError Validation message for the next-due-date field, or `null` when valid.
 * @param createdAt Timestamp the existing record was created; `null` for new records.
 * @param isLoading Whether the form is still loading an existing dentistry record.
 * @param isSaving Whether a save is currently in progress.
 */
data class DentistryFormState(
    val id: Long? = null,
    val date: String = todayIso(),
    val findings: String? = null,
    val treatment: String? = null,
    val nextDueDate: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val dateError: String? = null,
    val nextDueDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
