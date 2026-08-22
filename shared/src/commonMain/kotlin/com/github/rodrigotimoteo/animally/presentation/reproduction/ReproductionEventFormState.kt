package com.github.rodrigotimoteo.animally.presentation.reproduction

import kotlin.time.Instant

/**
 * UI state for the reproduction-event add/edit form.
 *
 * @param id The persisted reproduction-event id; `null` when creating a new one.
 * @param eventType The type of reproduction event.
 * @param date The event date as a display string (ISO `yyyy-MM-dd`).
 * @param details Optional additional details about the event.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param eventTypeError Validation message for the event-type field, or `null` when valid.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the persisted event, when editing.
 * @param isLoading Whether the form is still loading an existing event.
 * @param isSaving Whether a save is currently in progress.
 */
data class ReproductionEventFormState(
    val id: Long? = null,
    val eventType: String = "",
    val date: String = "",
    val details: String? = null,
    val initialExamFindings: String? = null,
    val stallionName: String? = null,
    val breedingType: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val eventTypeError: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
