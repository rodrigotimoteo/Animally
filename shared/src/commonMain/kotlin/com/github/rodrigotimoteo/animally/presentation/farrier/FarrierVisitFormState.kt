package com.github.rodrigotimoteo.animally.presentation.farrier

import kotlin.time.Instant

/**
 * UI state for the farrier visit add/edit form.
 *
 * @param id The persisted farrier visit id; `null` when creating a new one.
 * @param date The visit date as a display string (ISO `yyyy-MM-dd`).
 * @param trimOrShoe Optional trim or shoeing type.
 * @param shoeType Optional shoe type applied.
 * @param findings Optional findings from the examination.
 * @param nextDueDate Optional date of the next scheduled visit.
 * @param farrier Optional name of the farrier.
 * @param notes Optional free-form notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param nextDueDateError Validation message for the next-due-date field, or `null` when valid.
 * @param createdAt Timestamp the existing record was created; `null` for new records.
 * @param isLoading Whether the form is still loading an existing farrier visit.
 * @param isSaving Whether a save is currently in progress.
 */
data class FarrierVisitFormState(
    val id: Long? = null,
    val date: String = "",
    val trimOrShoe: String? = null,
    val shoeType: String? = null,
    val findings: String? = null,
    val nextDueDate: String? = null,
    val farrier: String? = null,
    val notes: String? = null,
    val dateError: String? = null,
    val nextDueDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
