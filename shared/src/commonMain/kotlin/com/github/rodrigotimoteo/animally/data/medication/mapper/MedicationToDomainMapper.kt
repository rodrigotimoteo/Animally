package com.github.rodrigotimoteo.animally.data.medication.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Medication
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication as DomainMedication

/**
 * Converts this persistence [Medication] to a domain [DomainMedication].
 *
 * @return mapped [DomainMedication]
 */
fun Medication.toDomain(): DomainMedication =
    DomainMedication(
        id = id,
        patientId = patientId,
        name = name,
        dosage = dosage,
        route = route,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        prescribedBy = prescribedBy,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
