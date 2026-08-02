package com.github.rodrigotimoteo.animally.domain.patient.usecase

import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Clock

/**
 * Use case for soft-deleting a patient by marking it inactive.
 *
 * Deletion is blocked while the patient still has active linked records;
 * [PatientHasRecordsException] is thrown in that case.
 *
 * @param patientRepository Repository instance for accessing patient data.
 * @param searchRepository Repository instance for the global search index.
 */
@Single
class DeletePatientUseCase(
    @Provided private val patientRepository: IPatientRepository,
    @Provided private val searchRepository: ISearchRepository,
) {
    /**
     * Marks the patient identified by [patientId] as inactive.
     *
     * @param patientId the identifier of the patient to delete.
     * @throws PatientHasRecordsException when the patient still has active linked records.
     */
    operator fun invoke(patientId: Long) {
        val recordCount = patientRepository.countActiveRecords(patientId)
        if (recordCount > 0) {
            throw PatientHasRecordsException(recordCount)
        }
        patientRepository.setInactive(patientId, Clock.System.now())
        searchRepository.deleteRecord(ISearchRepository.TYPE_PATIENT, patientId)
    }
}
