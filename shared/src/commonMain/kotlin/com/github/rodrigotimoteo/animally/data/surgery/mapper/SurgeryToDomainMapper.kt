package com.github.rodrigotimoteo.animally.data.surgery.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Surgery
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery as DomainSurgery

/**
 * Converts this persistence [Surgery] to a domain [DomainSurgery].
 *
 * @return mapped [DomainSurgery]
 */
fun Surgery.toDomain(): DomainSurgery =
    DomainSurgery(
        id = id,
        patientId = patientId,
        date = date,
        type = type,
        description = description,
        outcome = outcome,
        surgeon = surgeon,
        anesthesia = anesthesia,
        analgesia = analgesia,
        complications = complications,
        recoveryNotes = recoveryNotes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
