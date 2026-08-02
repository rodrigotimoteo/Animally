package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated consultation.
 *
 * A single save path for both create and edit flows: consultations with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param consultationRepository Repository instance for accessing consultation data.
 */
@Single
class SaveConsultationUseCase(
    @Provided private val consultationRepository: IConsultationRepository,
) {
    /**
     * Persists the given [consultation] and returns the generated identifier for new consultations.
     *
     * @param consultation the consultation to persist.
     * @return the id of the persisted consultation.
     */
    operator fun invoke(consultation: Consultation): Long =
        if (consultation.id == 0L) {
            consultationRepository.insert(consultation)
        } else {
            consultationRepository.update(consultation)
        }
}
