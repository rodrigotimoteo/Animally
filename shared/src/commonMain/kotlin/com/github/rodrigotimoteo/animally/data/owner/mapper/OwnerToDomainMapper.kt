package com.github.rodrigotimoteo.animally.data.owner.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Owner
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner as DomainOwner

/**
 * Converts this persistence [Owner] to a domain [DomainOwner].
 *
 * @return mapped [DomainOwner]
 */
fun Owner.toDomain(): DomainOwner =
    DomainOwner(
        id = id,
        name = name,
        email = email,
        phone = phone,
        address = address,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
