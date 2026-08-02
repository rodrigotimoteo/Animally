package com.github.rodrigotimoteo.animally.domain.patient.usecase

/**
 * Thrown when attempting to delete a patient that still has active linked records.
 *
 * @property recordCount the number of active records still linked to the patient.
 */
class PatientHasRecordsException(
    val recordCount: Long,
) : Exception("Patient has $recordCount records. Delete records first or use soft delete.")
