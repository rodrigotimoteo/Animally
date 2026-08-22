package com.github.rodrigotimoteo.animally.domain.embryotransfer.usecase

import com.github.rodrigotimoteo.animally.domain.embryotransfer.IEmbryoTransferRepository
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all active embryo transfer records of a patient.
 */
@Single
class GetEmbryoTransfersByPatientUseCase(
    @Provided private val repository: IEmbryoTransferRepository,
) {
    /**
     * Returns all active records for the patient with [patientId].
     */
    operator fun invoke(patientId: Long): List<EmbryoTransfer> = repository.getByPatient(patientId)
}
