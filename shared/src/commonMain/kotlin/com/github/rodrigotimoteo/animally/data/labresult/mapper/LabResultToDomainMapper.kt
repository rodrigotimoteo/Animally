package com.github.rodrigotimoteo.animally.data.labresult.mapper

import com.github.rodrigotimoteo.animally.data.migrations.LabResult
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult as DomainLabResult

/**
 * Converts this persistence [LabResult] to a domain [DomainLabResult].
 *
 * @return mapped [DomainLabResult]
 */
fun LabResult.toDomain(): DomainLabResult =
    DomainLabResult(
        id = id,
        patientId = patientId,
        testType = testType,
        date = date,
        results = results,
        normalRange = normalRange,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
