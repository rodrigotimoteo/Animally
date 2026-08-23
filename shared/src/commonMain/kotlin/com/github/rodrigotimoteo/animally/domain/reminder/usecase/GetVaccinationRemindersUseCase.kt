package com.github.rodrigotimoteo.animally.domain.reminder.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Collects vaccination reminders for every active patient.
 *
 * Each active vaccination with a [com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination.nextDueDate]
 * yields one [Reminder]. Overdue (due before [today]) and upcoming (due on or after [today]) are both
 * included. Results are sorted by due date.
 *
 * @param today Reference date used to classify reminders as overdue or upcoming.
 * @return The vaccination reminders, soonest due first.
 */
@Single
class GetVaccinationRemindersUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
    @Provided private val patientRepository: IPatientRepository,
) {
    operator fun invoke(today: LocalDate): List<Reminder> {
        val reminders =
            patientRepository
                .getPatientList()
                .flatMap { patient ->
                    vaccinationRepository
                        .getByPatient(patient.id)
                        .mapNotNull { vaccination ->
                            val dueDate = vaccination.nextDueDate
                            if (!vaccination.isActive || dueDate == null) {
                                null
                            } else {
                                Reminder(
                                    patientId = patient.id,
                                    patientName = patient.name,
                                    recordType = RECORD_TYPE,
                                    title = vaccination.vaccineName,
                                    dueDate = dueDate,
                                )
                            }
                        }
                }
        return reminders.sortedBy { it.dueDate }
    }

    private companion object {
        val RECORD_TYPE = RecordType.Vaccination.displayName
    }
}
