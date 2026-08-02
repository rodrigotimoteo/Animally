package com.github.rodrigotimoteo.animally.data.customreminder

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.customreminder.mapper.toDomain
import com.github.rodrigotimoteo.animally.domain.customreminder.ICustomReminderRepository
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single
import kotlin.time.Instant

/**
 * Repository implementation for managing [CustomReminder] records.
 */
@Single(binds = [ICustomReminderRepository::class])
class CustomReminderRepositoryImpl(
    @Provided private val database: AnimallyDatabase,
) : ICustomReminderRepository {
    private val customReminderQueries: CustomReminderQueries = database.customReminderQueries

    override fun getByPatient(patientId: Long): List<CustomReminder> =
        customReminderQueries
            .selectByPatient(patientId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: Long): CustomReminder? =
        customReminderQueries
            .selectById(id)
            .executeAsOneOrNull()
            ?.toDomain()

    override fun getUpcoming(today: LocalDate): List<CustomReminder> =
        customReminderQueries
            .selectUpcoming(today)
            .executeAsList()
            .map { it.toDomain() }

    override fun getOverdue(today: LocalDate): List<CustomReminder> =
        customReminderQueries
            .selectOverdue(today)
            .executeAsList()
            .map { it.toDomain() }

    override fun insert(customReminder: CustomReminder): Long =
        customReminderQueries
            .insert(
                patientId = customReminder.patientId,
                title = customReminder.title,
                dueDate = customReminder.dueDate,
                linkedRecordType = customReminder.linkedRecordType,
                linkedRecordId = customReminder.linkedRecordId,
                notes = customReminder.notes,
                isActive = customReminder.isActive,
                createdAt = customReminder.createdAt,
                updatedAt = customReminder.updatedAt,
            ).value

    override fun update(customReminder: CustomReminder): Long =
        customReminderQueries
            .update(
                id = customReminder.id,
                patientId = customReminder.patientId,
                title = customReminder.title,
                dueDate = customReminder.dueDate,
                linkedRecordType = customReminder.linkedRecordType,
                linkedRecordId = customReminder.linkedRecordId,
                notes = customReminder.notes,
                isActive = customReminder.isActive,
                updatedAt = customReminder.updatedAt,
            ).value

    override fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long =
        customReminderQueries
            .setInactive(
                id = id,
                updatedAt = updatedAt,
            ).value
}
