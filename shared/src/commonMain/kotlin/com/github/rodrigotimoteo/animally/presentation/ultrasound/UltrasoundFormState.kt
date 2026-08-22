package com.github.rodrigotimoteo.animally.presentation.ultrasound

import kotlin.time.Instant

/**
 * One editable follicle row in the ultrasound form.
 *
 * @property id Persisted follicle id; `0` for a not-yet-saved row.
 * @property sizeMm Follicle size in millimeters as a display string.
 * @property description Optional free-form description of the follicle.
 */
data class FollicleRow(
    val id: Long = 0L,
    val sizeMm: String = "",
    val description: String? = null,
)

/**
 * UI state for the ultrasound add/edit form.
 *
 * @param id The persisted ultrasound id; `null` when creating a new one.
 * @param date The examination date as a display string (ISO `yyyy-MM-dd`).
 * @param ovaryStatus Optional status of the ovaries.
 * @param uterineStatus Optional status of the uterus.
 * @param follicleSizeMm Optional follicle size in millimeters as a display string.
 * @param leftFollicles Editable follicle rows observed on the left ovary.
 * @param rightFollicles Editable follicle rows observed on the right ovary.
 * @param findings Optional findings from the examination.
 * @param imageUris Optional comma-separated URIs of attached images.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param follicleSizeMmError Validation message for the follicle-size field, or `null` when valid.
 * @param createdAt The creation timestamp of the persisted ultrasound, when editing.
 * @param isLoading Whether the form is still loading an existing ultrasound.
 * @param isSaving Whether a save is currently in progress.
 */
data class UltrasoundFormState(
    val id: Long? = null,
    val date: String = "",
    val ovaryStatus: String? = null,
    val uterineStatus: String? = null,
    val follicleSizeMm: String? = null,
    val leftOvaryStatus: String? = null,
    val rightOvaryStatus: String? = null,
    val leftFollicleSizeMm: String? = null,
    val rightFollicleSizeMm: String? = null,
    val leftFollicles: List<FollicleRow> = emptyList(),
    val rightFollicles: List<FollicleRow> = emptyList(),
    val uterineEdema: String? = null,
    val uterineLiquid: Boolean? = null,
    val uterineLiquidDescription: String? = null,
    val uterusDescription: String? = null,
    val findings: String? = null,
    val imageUris: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val dateError: String? = null,
    val follicleSizeMmError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
