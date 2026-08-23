package com.github.rodrigotimoteo.animally.domain.care

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Aggregates upcoming and overdue care for one patient into a single due list.
 *
 * Sources: vaccination next-due dates, dentistry next-due dates, farrier visit
 * next-due dates, gestation expected foaling dates, and custom reminder due
 * dates. Only dates within [windowDays] of [today] (or already past) are
 * returned, sorted by due date ascending so the most urgent item is first.
 *
 * @param vaccinationRepository Source of vaccination next-due dates.
 * @param dentistryRepository Source of dentistry next-due dates.
 * @param farrierVisitRepository Source of farrier visit next-due dates.
 * @param gestationRepository Source of expected foaling dates.
 * @param customReminderRepository Source of custom reminder due dates.
 */
@Single
class GetUpcomingRemindersUseCase(
    @Provided private val vaccinationRepository: IVaccinationRepository,
    @Provided private val dentistryRepository: IDentistryRepository,
    @Provided private val farrierVisitRepository: IFarrierVisitRepository,
    @Provided private val gestationRepository: IGestationRepository,
    @Provided private val customReminderRepository: ICustomReminderRepository,
) {
    /**
     * Builds the care-due list for the patient with [patientId].
     *
     * @param patientId The patient whose records are scanned.
     * @param today Reference date for the overdue flag and the inclusion window.
     * @param windowDays How many days ahead of [today] to include. Past-due
     *   items are always included regardless of this window.
     * @return The due items, soonest first.
     */
    operator fun invoke(
        patientId: Long,
        today: LocalDate,
        windowDays: Int = DEFAULT_WINDOW_DAYS,
    ): List<CareDueItem> {
        val horizon = today.plus(DatePeriod(days = windowDays))
        val items =
            buildList {
                vaccinationRepository.getByPatient(patientId).forEach { vaccination ->
                    vaccination.nextDueDate?.takeIf { it <= horizon }?.let { due ->
                        add(CareDueItem(TYPE_VACCINATION, vaccination.vaccineName, due, due < today))
                    }
                }
                dentistryRepository.getByPatient(patientId).forEach { dentistry ->
                    dentistry.nextDueDate?.takeIf { it <= horizon }?.let { due ->
                        val title = dentistry.treatment?.takeIf { it.isNotBlank() } ?: TITLE_DENTISTRY
                        add(CareDueItem(TYPE_DENTISTRY, title, due, due < today))
                    }
                }
                farrierVisitRepository.getByPatient(patientId).forEach { farrierVisit ->
                    farrierVisit.nextDueDate?.takeIf { it <= horizon }?.let { due ->
                        val title = farrierVisit.trimOrShoe?.takeIf { it.isNotBlank() } ?: TITLE_FARRIER
                        add(CareDueItem(TYPE_FARRIER, title, due, due < today))
                    }
                }
                gestationRepository.getByPatient(patientId).forEach { gestation ->
                    val due = gestation.expectedDueDate
                    if (due <= horizon) {
                        add(CareDueItem(TYPE_GESTATION, TITLE_GESTATION, due, due < today))
                    }
                }
                customReminderRepository.getByPatient(patientId).forEach { reminder ->
                    if (reminder.dueDate <= horizon) {
                        add(CareDueItem(TYPE_REMINDER, reminder.title, reminder.dueDate, reminder.dueDate < today))
                    }
                }
            }
        return items.sortedBy { it.dueDate }
    }

    private companion object {
        const val DEFAULT_WINDOW_DAYS = 30

        const val TYPE_VACCINATION = "Vaccination"
        const val TYPE_DENTISTRY = "Dentistry"
        const val TYPE_FARRIER = "Farrier"
        const val TYPE_GESTATION = "Gestation"
        const val TYPE_REMINDER = "Reminder"

        const val TITLE_DENTISTRY = "Dental check"
        const val TITLE_FARRIER = "Farrier visit"
        const val TITLE_GESTATION = "Expected foaling"
    }
}
