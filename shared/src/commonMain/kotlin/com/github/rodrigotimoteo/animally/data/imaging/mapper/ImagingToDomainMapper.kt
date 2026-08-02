package com.github.rodrigotimoteo.animally.data.imaging.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Imaging
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging as DomainImaging

/**
 * Converts this persistence [Imaging] to a domain [DomainImaging].
 *
 * @return mapped [DomainImaging]
 */
fun Imaging.toDomain(): DomainImaging =
    DomainImaging(
        id = id,
        patientId = patientId,
        type = type.orEmpty(),
        date = date,
        findings = findings,
        imageUris = imageUris,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
