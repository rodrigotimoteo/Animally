package com.github.rodrigotimoteo.animally.data.anamnese.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Anamnese
import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese as DomainAnamnese

/**
 * Converts this persistence [Anamnese] to a domain [DomainAnamnese].
 *
 * @return mapped [DomainAnamnese]
 */
fun Anamnese.toDomain(): DomainAnamnese =
    DomainAnamnese(
        id = id,
        patientId = patientId,
        generalHistory = generalHistory.orEmpty(),
        chronicConditions = chronicConditions.orEmpty(),
        allergies = allergies.orEmpty(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
