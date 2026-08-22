package com.github.rodrigotimoteo.animally.domain.icsi.usecase

import com.github.rodrigotimoteo.animally.domain.icsi.IIcsiRepository
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all active ICSI records of a patient.
 */
@Single
class GetIcsiByPatientUseCase(
    @Provided private val repository: IIcsiRepository,
) {
    /**
     * Returns all active records for the patient with [patientId].
     */
    operator fun invoke(patientId: Long): List<Icsi> = repository.getByPatient(patientId)
}
