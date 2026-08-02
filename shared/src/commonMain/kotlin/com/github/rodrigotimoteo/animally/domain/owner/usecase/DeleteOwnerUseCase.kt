package com.github.rodrigotimoteo.animally.domain.owner.usecase

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting an owner by marking it inactive.
 *
 * Deletion is blocked while the owner still has active patients assigned;
 * [OwnerHasPatientsException] is thrown in that case.
 *
 * @param ownerRepository Repository instance for accessing owner data.
 * @param patientRepository Repository instance for checking linked patients.
 */
@Single
class DeleteOwnerUseCase(
    @Provided private val ownerRepository: IOwnerRepository,
    @Provided private val patientRepository: IPatientRepository,
) {
    /**
     * Marks the owner identified by [ownerId] as inactive.
     *
     * @param ownerId the identifier of the owner to delete.
     * @throws OwnerHasPatientsException when the owner still has active patients.
     */
    operator fun invoke(ownerId: Long) {
        val patientCount = patientRepository.countPatientsByOwnerId(ownerId)
        if (patientCount > 0) {
            throw OwnerHasPatientsException(patientCount)
        }
        ownerRepository.setInactive(ownerId, Clock.System.now())
    }
}
