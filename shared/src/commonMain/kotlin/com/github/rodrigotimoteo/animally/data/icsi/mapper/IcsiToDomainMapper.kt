package com.github.rodrigotimoteo.animally.data.icsi.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Icsi
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi as DomainIcsi

/**
 * Converts this persistence [Icsi] to a domain [DomainIcsi].
 *
 * @return mapped [DomainIcsi]
 */
fun Icsi.toDomain(): DomainIcsi =
    DomainIcsi(
        id = id,
        patientId = patientId,
        date = date,
        folliclesRecovered = folliclesRecovered.toInt(),
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        serverId = serverId,
    )
