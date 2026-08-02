package com.github.rodrigotimoteo.animally.data.deworming.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Deworming
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming as DomainDeworming

/**
 * Converts this persistence [Deworming] to a domain [DomainDeworming].
 *
 * @return mapped [DomainDeworming]
 */
fun Deworming.toDomain(): DomainDeworming =
    DomainDeworming(
        id = id,
        patientId = patientId,
        product = product,
        dateAdministered = dateAdministered,
        nextDueDate = nextDueDate,
        dose = dose,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
