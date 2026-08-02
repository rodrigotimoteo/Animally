package com.github.rodrigotimoteo.animally.data.repromedication.mapper

import com.github.rodrigotimoteo.animally.data.migrations.ReproMedication
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication as DomainReproMedication

/**
 * Converts this persistence [ReproMedication] to a domain [DomainReproMedication].
 *
 * @return mapped [DomainReproMedication]
 */
fun ReproMedication.toDomain(): DomainReproMedication =
    DomainReproMedication(
        id = id,
        patientId = patientId,
        medication = medication,
        dateAdministered = dateAdministered,
        dosage = dosage,
        purpose = purpose,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
