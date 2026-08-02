package com.github.rodrigotimoteo.animally.data.gestation.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation as DomainGestation

/**
 * Converts this persistence [Gestation] to a domain [DomainGestation].
 *
 * @return mapped [DomainGestation]
 */
fun Gestation.toDomain(): DomainGestation =
    DomainGestation(
        id = id,
        patientId = patientId,
        breedingDate = breedingDate,
        expectedDueDate = expectedDueDate,
        gestationDays = gestationDays.toInt(),
        status = status.orEmpty(),
        fetalCount = fetalCount?.toInt(),
        lastCheckDate = lastCheckDate,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
