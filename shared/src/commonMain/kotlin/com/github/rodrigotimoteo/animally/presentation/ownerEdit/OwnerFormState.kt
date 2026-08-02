package com.github.rodrigotimoteo.animally.presentation.ownerEdit

import kotlin.time.Instant

/**
 * UI state for the owner add/edit form.
 *
 * @param id The persisted owner id; `null` when creating a new owner.
 * @param name The owner's full name.
 * @param phone Optional phone number.
 * @param email Optional email address.
 * @param address Optional physical address.
 * @param createdAt The original creation timestamp, preserved when editing.
 * @param nameError Validation message for the name field, or `null` when valid.
 * @param isLoading Whether the form is still loading an existing owner.
 * @param isSaving Whether a save is currently in progress.
 */
data class OwnerFormState(
    val id: Long? = null,
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val createdAt: Instant? = null,
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = id != null
}
