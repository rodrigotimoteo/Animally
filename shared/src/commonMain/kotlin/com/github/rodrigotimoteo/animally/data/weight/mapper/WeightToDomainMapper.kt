package com.github.rodrigotimoteo.animally.data.weight.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Weight
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight as DomainWeight

/**
 * Converts this persistence [Weight] to a domain [DomainWeight].
 *
 * @return mapped [DomainWeight]
 */
fun Weight.toDomain(): DomainWeight =
    DomainWeight(
        id = id,
        patientId = patientId,
        weightKg = weightKg,
        date = date,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
