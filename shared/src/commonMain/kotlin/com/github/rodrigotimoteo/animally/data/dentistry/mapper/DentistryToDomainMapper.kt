package com.github.rodrigotimoteo.animally.data.dentistry.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Dentistry
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry as DomainDentistry

/**
 * Converts this persistence [Dentistry] to a domain [DomainDentistry].
 *
 * @return mapped [DomainDentistry]
 */
fun Dentistry.toDomain(): DomainDentistry =
    DomainDentistry(
        id = id,
        patientId = patientId,
        date = date,
        findings = findings,
        treatment = treatment,
        nextDueDate = nextDueDate,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
