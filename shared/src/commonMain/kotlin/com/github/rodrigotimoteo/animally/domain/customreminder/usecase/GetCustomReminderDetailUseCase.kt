package com.github.rodrigotimoteo.animally.domain.customreminder.usecase

import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

/**
 * Use case for retrieving detailed information about a specific custom reminder.
 *
 * @param customReminderRepository Repository instance for accessing custom reminder data.
 */
@Single
class GetCustomReminderDetailUseCase(
    @Provided private val customReminderRepository: ICustomReminderRepository,
) {
    /**
     * Retrieves detailed information for a custom reminder by its ID.
     *
     * @param id The unique identifier of the custom reminder to retrieve.
     * @return The [CustomReminder] object if found, or `null` if no reminder with the given ID exists.
     */
    operator fun invoke(id: Long): CustomReminder? = customReminderRepository.getById(id)
}
