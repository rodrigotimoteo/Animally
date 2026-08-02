package com.github.rodrigotimoteo.animally.presentation.surgery

import kotlin.time.Instant

/**
 * UI state for the surgery add/edit form.
 *
 * @param id The persisted surgery id; `null` when creating a new one.
 * @param date The surgery date as a display string (ISO `yyyy-MM-dd`).
 * @param type Optional type of surgery performed.
 * @param description Optional description of the procedure.
 * @param outcome Optional post-surgery outcome.
 * @param surgeon Optional name of the surgeon.
 * @param anesthesia Optional anesthesia details.
 * @param analgesia Optional analgesia details.
 * @param complications Optional complications encountered.
 * @param recoveryNotes Optional recovery notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing surgery.
 * @param isSaving Whether a save is currently in progress.
 */
data class SurgeryFormState(
    val id: Long? = null,
    val date: String = "",
    val type: String? = null,
    val description: String? = null,
    val outcome: String? = null,
    val surgeon: String? = null,
    val anesthesia: String? = null,
    val analgesia: String? = null,
    val complications: String? = null,
    val recoveryNotes: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
