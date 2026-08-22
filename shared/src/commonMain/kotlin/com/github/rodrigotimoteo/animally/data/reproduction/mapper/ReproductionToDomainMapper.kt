package com.github.rodrigotimoteo.animally.data.reproduction.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Reproduction
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent as DomainReproductionEvent

/**
 * Converts this persistence [Reproduction] to a domain [DomainReproductionEvent].
 *
 * @return mapped [DomainReproductionEvent]
 */
fun Reproduction.toDomain(): DomainReproductionEvent =
    DomainReproductionEvent(
        id = id,
        patientId = patientId,
        eventType = eventType,
        date = date,
        details = details,
        initialExamFindings = initialExamFindings,
        stallionName = stallionName,
        breedingType = breedingType,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
