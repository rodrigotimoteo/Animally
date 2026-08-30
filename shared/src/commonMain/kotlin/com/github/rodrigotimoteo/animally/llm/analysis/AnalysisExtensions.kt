package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent

internal fun pluralize(
    word: String,
    count: Int,
): String = if (count == 1) word else "${word}s"

private const val RESOLVED_STATUS_COMPLETED = "Completed"
private const val RESOLVED_STATUS_FAILED = "Failed"
private const val RESOLVED_STATUS_FOALED = "Foaled"

internal fun Gestation.isResolved(): Boolean =
    status.equals(RESOLVED_STATUS_COMPLETED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FAILED, ignoreCase = true) ||
        status.equals(RESOLVED_STATUS_FOALED, ignoreCase = true)

internal fun Gestation.isActiveGestation(): Boolean = isActive && !isResolved()

internal fun ReproductionEvent.isBreedingEvent(): Boolean {
    val normalized = eventType.trim().lowercase()
    return normalized == "breeding" ||
        normalized == "mating" ||
        normalized == "insemination" ||
        normalized == "cobertura" ||
        normalized == "cobrição" ||
        normalized == "cobricao"
}
