package com.github.rodrigotimoteo.animally.presentation.consultation

import kotlin.time.Instant

/**
 * UI state for the consultation add/edit form.
 *
 * @param id The persisted consultation id; `null` when creating a new one.
 * @param date The consultation date as a display string (ISO `yyyy-MM-dd`).
 * @param subjective SOAP Subjective — the owner's description of the issue.
 * @param objective SOAP Objective — the exam findings.
 * @param assessment SOAP Assessment — the diagnosis.
 * @param plan SOAP Plan — the treatment.
 * @param vetName Optional name of the attending veterinarian.
 * @param nextVisitDate Optional date of the next scheduled visit.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param nextVisitDateError Validation message for the next-visit-date field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing consultation.
 * @param isSaving Whether a save is currently in progress.
 */
data class ConsultationFormState(
    val id: Long? = null,
    val date: String = "",
    val subjective: String = "",
    val objective: String = "",
    val assessment: String = "",
    val plan: String = "",
    val vetName: String? = null,
    val nextVisitDate: String? = null,
    val dateError: String? = null,
    val nextVisitDateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
