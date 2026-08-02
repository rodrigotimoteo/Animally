package com.github.rodrigotimoteo.animally.domain.owner.usecase

/**
 * Thrown when attempting to delete an owner that still has active patients assigned.
 *
 * @property patientCount the number of active patients still linked to the owner.
 */
class OwnerHasPatientsException(
    val patientCount: Long,
) : Exception("Owner has $patientCount patients. Unassign first.")
