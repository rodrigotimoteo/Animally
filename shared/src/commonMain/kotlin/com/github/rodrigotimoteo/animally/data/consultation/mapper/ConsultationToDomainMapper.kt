package com.github.rodrigotimoteo.animally.data.consultation.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Consultation
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation as DomainConsultation

/**
 * Converts this persistence [Consultation] to a domain [DomainConsultation].
 *
 * @return mapped [DomainConsultation]
 */
fun Consultation.toDomain(): DomainConsultation =
    DomainConsultation(
        id = id,
        patientId = patientId,
        date = date,
        subjective = subjective.orEmpty(),
        objective = objective.orEmpty(),
        assessment = assessment.orEmpty(),
        plan = plan.orEmpty(),
        vetName = vetName,
        nextVisitDate = nextVisitDate,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
