package com.github.rodrigotimoteo.animally.data.substance.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Substance
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance

/**
 * Converts this persistence [Substance] to a domain [ControlledSubstance].
 *
 * @return mapped [ControlledSubstance]
 */
fun Substance.toDomain(): ControlledSubstance =
    ControlledSubstance(
        id = id,
        patientId = patientId,
        drugName = drugName,
        dose = dose,
        unit = unit,
        route = route,
        administeredBy = administeredBy,
        witness = witness,
        date = date,
        reason = reason,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
