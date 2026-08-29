package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent

/** Database-backed reproduction-card fact used for exact field answers. */
internal data class ReproductionAttributeFact(
    val patient: Patient,
    val event: ReproductionEvent,
)
