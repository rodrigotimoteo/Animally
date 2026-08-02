package com.github.rodrigotimoteo.animally.data.lameness.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Lameness
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness as DomainLameness

/**
 * Converts this persistence [Lameness] to a domain [DomainLameness].
 *
 * @return mapped [DomainLameness]
 */
fun Lameness.toDomain(): DomainLameness =
    DomainLameness(
        id = id,
        patientId = patientId,
        date = date,
        gradeAAEP = (gradeAAEP ?: 0).toInt(),
        limbLocation = limbLocation,
        flexionTest = flexionTest,
        diagnosis = diagnosis,
        treatment = treatment,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
