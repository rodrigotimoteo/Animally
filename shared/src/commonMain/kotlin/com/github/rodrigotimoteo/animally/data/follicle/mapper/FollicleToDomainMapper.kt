package com.github.rodrigotimoteo.animally.data.follicle.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Follicle
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle as DomainFollicle

/**
 * Converts this persistence [Follicle] to a domain [DomainFollicle].
 *
 * @return mapped [DomainFollicle]
 */
fun Follicle.toDomain(): DomainFollicle =
    DomainFollicle(
        id = id,
        ultrasoundId = ultrasoundId,
        side = side,
        sizeMm = sizeMm,
        description = description,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        serverId = serverId,
    )
