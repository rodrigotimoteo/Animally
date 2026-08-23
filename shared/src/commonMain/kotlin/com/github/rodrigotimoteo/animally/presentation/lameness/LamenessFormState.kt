
package com.github.rodrigotimoteo.animally.presentation.lameness

import com.github.rodrigotimoteo.animally.presentation.common.todayIso
import kotlin.time.Instant

/**
 * UI state for the lameness add/edit form.
 *
 * @param id The persisted lameness id; `null` when creating a new one.
 * @param date The evaluation date as a display string (ISO `yyyy-MM-dd`).
 * @param gradeAAEP AAEP lameness grade on the 1-5 scale as a display string.
 * @param limbLocation Optional affected limb location.
 * @param flexionTest Optional flexion test result.
 * @param diagnosis Optional diagnosis.
 * @param treatment Optional treatment plan.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional clinical notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param gradeError Validation message for the grade field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing lameness evaluation.
 * @param isSaving Whether a save is currently in progress.
 */
data class LamenessFormState(
    val id: Long? = null,
    val date: String = todayIso(),
    val gradeAAEP: String = "",
    val limbLocation: String? = null,
    val flexionTest: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val dateError: String? = null,
    val gradeError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
