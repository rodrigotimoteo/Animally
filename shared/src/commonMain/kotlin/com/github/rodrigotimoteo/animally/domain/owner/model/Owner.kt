package com.github.rodrigotimoteo.animally.domain.owner.model

import kotlin.time.Instant

/**
 * Domain model representing an owner/guardian of animals.
 *
 * This class encapsulates the essential information about an owner, including
 * personal contact details and system metadata for tracking lifecycle events.
 *
 * @property id Unique identifier for the owner.
 * @property name The owner's full name.
 * @property email Optional email address for contact.
 * @property phone Optional phone number for contact.
 * @property address Optional physical address.
 * @property isActive Indicates whether the owner record is active. Defaults to `true`.
 * @property createdAt Timestamp when the owner record was created.
 * @property updatedAt Timestamp when the owner record was last modified.
 */
data class Owner(
    val id: Long,
    val name: String,
    val email: String?,
    val phone: String?,
    val address: String?,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
)
