package com.github.rodrigotimoteo.animally.domain.consultation.usecase

import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for persisting a new or updated consultation.
 *
 * A single save path for both create and edit flows: consultations with `id == 0L`
 * are inserted, all others are updated.
 *
 * @param consultationRepository Repository instance for accessing consultation data.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class SaveConsultationUseCase(
    @Provided private val consultationRepository: IConsultationRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Persists the given [consultation] and returns the generated identifier for new consultations.
     *
     * @param consultation the consultation to persist.
     * @return the id of the persisted consultation.
     */
    operator fun invoke(consultation: Consultation): Long {
        val savedId =
            if (consultation.id == 0L) {
                consultationRepository.insert(consultation)
            } else {
                consultationRepository.update(consultation)
                consultation.id
            }
        val searchableText = listOfNotNull(consultation.assessment, consultation.plan).joinToString(" ")
        searchRepository.indexRecord(
            recordType = ISearchRepository.TYPE_CONSULTATION,
            patientId = consultation.patientId,
            recordId = savedId,
            date = consultation.date,
            searchableText = searchableText,
        )
        return savedId
    }
}
