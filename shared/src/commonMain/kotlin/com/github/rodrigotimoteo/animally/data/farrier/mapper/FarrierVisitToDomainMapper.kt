package com.github.rodrigotimoteo.animally.data.farrier.mapper

import com.github.rodrigotimoteo.animally.data.migrations.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit as DomainFarrierVisit

/**
 * Converts this persistence [FarrierVisit] to a domain [DomainFarrierVisit].
 *
 * @return mapped [DomainFarrierVisit]
 */
fun FarrierVisit.toDomain(): DomainFarrierVisit =
    DomainFarrierVisit(
        id = id,
        patientId = patientId,
        date = date,
        trimOrShoe = trimOrShoe,
        shoeType = shoeType,
        findings = findings,
        nextDueDate = nextDueDate,
        farrier = farrier,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
