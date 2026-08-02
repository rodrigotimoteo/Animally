package com.github.rodrigotimoteo.animally.data.customreminder.mapper

import com.github.rodrigotimoteo.animally.data.migrations.CustomReminder
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder as DomainCustomReminder

/**
 * Converts this persistence [CustomReminder] to a domain [DomainCustomReminder].
 *
 * @return mapped [DomainCustomReminder]
 */
fun CustomReminder.toDomain(): DomainCustomReminder =
    DomainCustomReminder(
        id = id,
        patientId = patientId,
        title = title,
        dueDate = dueDate,
        linkedRecordType = linkedRecordType,
        linkedRecordId = linkedRecordId,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
