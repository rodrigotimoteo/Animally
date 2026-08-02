package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific consultation.
 *
 * @param consultationRepository Repository instance for accessing consultation data.
 */
@Single
class GetConsultationDetailUseCase(
    @Provided private val consultationRepository: IConsultationRepository,
) {
    /**
     * Retrieves detailed information for a consultation by its ID.
     *
     * @param id The unique identifier of the consultation to retrieve.
     * @return The [Consultation] object if found, or `null` if no consultation with the given ID exists.
     */
    operator fun invoke(id: Long): Consultation? = consultationRepository.getById(id)
}
