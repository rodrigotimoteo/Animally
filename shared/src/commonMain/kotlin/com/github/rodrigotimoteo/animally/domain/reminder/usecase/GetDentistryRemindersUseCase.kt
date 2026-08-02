package com.github.rodrigotimoteo.animally.domain.reminder.usecase

import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.reminder.model.Reminder
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Collects dentistry reminders for every active patient.
 *
 * The dentistry next-due date is entered manually (3/6/9/12 month schedule), so the reminder
 * reads it directly without calculation. Each active record with a next due date yields one
 * [Reminder]. Overdue and upcoming records are both included, sorted by due date.
 *
 * @param today Reference date used to classify reminders as overdue or upcoming.
 * @return The dentistry reminders, soonest due first.
 */
@Single
class GetDentistryRemindersUseCase(
    @Provided private val dentistryRepository: IDentistryRepository,
    @Provided private val patientRepository: IPatientRepository,
) {
    operator fun invoke(today: LocalDate): List<Reminder> {
        val reminders =
            patientRepository
                .getPatientList()
                .flatMap { patient ->
                    dentistryRepository
                        .getByPatient(patient.id)
                        .mapNotNull { dentistry ->
                            val dueDate = dentistry.nextDueDate
                            if (!dentistry.isActive || dueDate == null) {
                                null
                            } else {
                                Reminder(
                                    patientId = patient.id,
                                    patientName = patient.name,
                                    recordType = RECORD_TYPE,
                                    title = TITLE,
                                    dueDate = dueDate,
                                )
                            }
                        }
                }
        return reminders.sortedBy { it.dueDate }
    }

    private companion object {
        const val RECORD_TYPE = "Dentistry"

        const val TITLE = "Dental check"
    }
}
