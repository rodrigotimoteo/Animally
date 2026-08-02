package com.github.rodrigotimoteo.animally.data.vaccination.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination as DomainVaccination

/**
 * Converts this persistence [Vaccination] to a domain [DomainVaccination].
 *
 * @return mapped [DomainVaccination]
 */
fun Vaccination.toDomain(): DomainVaccination =
    DomainVaccination(
        id = id,
        patientId = patientId,
        vaccineName = vaccineName,
        dateAdministered = dateAdministered,
        nextDueDate = nextDueDate,
        vetName = vetName,
        batchNumber = batchNumber,
        site = site,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
