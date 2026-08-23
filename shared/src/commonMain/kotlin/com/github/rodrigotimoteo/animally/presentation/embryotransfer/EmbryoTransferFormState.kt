
package com.github.rodrigotimoteo.animally.presentation.embryotransfer

import com.github.rodrigotimoteo.animally.presentation.common.todayIso
import kotlin.time.Instant

/**
 * UI state for the embryo transfer add/edit form.
 *
 * @param id The persisted record id; `null` when creating a new one.
 * @param date The procedure date as a display string (ISO `yyyy-MM-dd`).
 * @param embryoCount Number of collected embryos as a display string.
 * @param recipientMares Names of the recipient mares that received embryos.
 * @param vetName Optional name of the attending veterinarian.
 * @param notes Optional free-form notes.
 * @param dateError Validation message for the date field, or `null` when valid.
 * @param createdAt The creation timestamp of the persisted record, when editing.
 * @param isLoading Whether the form is still loading an existing record.
 * @param isSaving Whether a save is currently in progress.
 */
data class EmbryoTransferFormState(
    val id: Long? = null,
    val date: String = todayIso(),
    val embryoCount: String = "0",
    val recipientMares: String? = null,
    val vetName: String? = null,
    val notes: String? = null,
    val dateError: String? = null,
    val createdAt: Instant? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
