package com.github.rodrigotimoteo.animally.llm

/**
 * Deterministic intent pre-classifier for the dosage guardrail.
 *
 * Questions asking HOW MUCH of a drug to give must never reach the model
 * without medication records in context - a small on-device model with no
 * grounding will hallucinate a plausible-sounding dose. Kept apart from
 * [AssistantPrompts] so the prompt/query-shaping object stays within its
 * function-count budget.
 */
object DosageGuard {
    // Word-boundary regex so "mg" matches in "500 mg" but not inside longer
    // words.
    private val dosageIntentRegex =
        Regex(
            "\\b(how much|dose|doses|dosage|dosages|dosing|mg|ml|" +
                "administer|administers|administered|administering)\\b",
        )

    // Measurement intents that legitimately use "how much" and must NOT trip
    // the guardrail ("How much does Thunder weigh?").
    private val measurementIntentRegex =
        Regex("\\b(weigh|weighs|weighing|weight|weights|temperature|temperatures|fever)\\b")

    /**
     * True when [query] asks about drug dosages. Weight/temperature phrasings
     * that legitimately use "how much" are excluded.
     */
    fun isDosageIntent(query: String): Boolean {
        val lowered = query.lowercase()
        if (measurementIntentRegex.containsMatchIn(lowered)) return false
        return dosageIntentRegex.containsMatchIn(lowered)
    }
}
