package com.github.rodrigotimoteo.animally.data.embryotransfer.mapper

import com.github.rodrigotimoteo.animally.data.migrations.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer as DomainEmbryoTransfer

/**
 * Converts this persistence [EmbryoTransfer] to a domain [DomainEmbryoTransfer].
 *
 * @return mapped [DomainEmbryoTransfer]
 */
fun EmbryoTransfer.toDomain(): DomainEmbryoTransfer =
    DomainEmbryoTransfer(
        id = id,
        patientId = patientId,
        date = date,
        embryoCount = embryoCount.toInt(),
        recipientMares = recipientMares,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        serverId = serverId,
    )
