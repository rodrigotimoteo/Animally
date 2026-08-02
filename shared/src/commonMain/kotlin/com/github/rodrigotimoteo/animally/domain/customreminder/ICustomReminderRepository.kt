package com.github.rodrigotimoteo.animally.domain.customreminder

import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Repository contract for accessing [CustomReminder] records.
 */
interface ICustomReminderRepository {
    /**
     * Returns all active custom reminders for the patient with the given [patientId].
     *
     * @param patientId the patient identifier to look up.
     * @return the list of matching active custom reminders.
     */
    fun getByPatient(patientId: Long): List<CustomReminder>

    /**
     * Returns the active custom reminder with the given [id], or `null` when not found.
     *
     * @param id the custom reminder identifier to look up.
     * @return the matching custom reminder, or `null` if none exists.
     */
    fun getById(id: Long): CustomReminder?

    /**
     * Returns all active custom reminders due on or after [today], ordered by due date.
     *
     * @param today the reference date.
     * @return the list of upcoming active custom reminders.
     */
    fun getUpcoming(today: LocalDate): List<CustomReminder>

    /**
     * Returns all active custom reminders due before [today], ordered by due date.
     *
     * @param today the reference date.
     * @return the list of overdue active custom reminders.
     */
    fun getOverdue(today: LocalDate): List<CustomReminder>

    /**
     * Inserts [customReminder] into persistence and returns the generated identifier.
     *
     * @param customReminder the custom reminder to persist.
     * @return the id of the inserted custom reminder.
     */
    fun insert(customReminder: CustomReminder): Long

    /**
     * Updates the persisted data for [customReminder].
     *
     * @param customReminder the custom reminder containing the updated data.
     * @return the number of rows affected.
     */
    fun update(customReminder: CustomReminder): Long

    /**
     * Marks the custom reminder identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the custom reminder to deactivate.
     * @param updatedAt the current date and time.
     * @return the number of rows affected.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
