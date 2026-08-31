package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.gestation.model.isResolvedGestationStatus
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent

internal fun pluralize(
    word: String,
    count: Int,
): String = if (count == 1) word else "${word}s"

internal fun Gestation.isResolved(): Boolean = status.isResolvedGestationStatus()

internal fun ReproductionEvent.isBreedingEvent(): Boolean {
    val normalized = eventType.trim().lowercase()
    return normalized == "breeding" ||
        normalized == "mating" ||
        normalized == "insemination" ||
        normalized == "cobertura" ||
        normalized == "cobrição" ||
        normalized == "cobricao"
}
