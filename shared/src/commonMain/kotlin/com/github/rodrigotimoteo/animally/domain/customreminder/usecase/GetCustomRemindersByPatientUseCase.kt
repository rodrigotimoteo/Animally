package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving all custom reminders of a patient.
 *
 * @param customReminderRepository Repository instance for accessing custom reminder data.
 */
@Single
class GetCustomRemindersByPatientUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
) {
    /**
     * Retrieves all active custom reminders for the patient with the given [patientId].
     *
     * @param patientId The identifier of the patient.
     * @return The list of matching [CustomReminder] objects.
     */
    operator fun invoke(patientId: Long): List<CustomReminder> = customReminderRepository.getByPatient(patientId)
}
