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
    // Word-boundary regex so units match in "500 mg" but not inside longer
    // words. A quantity phrase by itself is not enough: "How much hay should
    // I give?" is husbandry, not a medication-dosage request.
    private val dosageCueRegex =
        Regex(
            "\\b(dose|doses|dosage|dosages|dosing|dosagem|posologia|mg|ml|mcg|g|" +
                "administered|administrado|administração|administracao)\\b",
        )

    private val quantityIntentRegex =
        Regex("\\b(how much|how many|quanto|quanta|quantos|quantas)\\b")

    private val medicationCueRegex =
        Regex(
            "\\b(medication|medications|medicine|medicines|drug|drugs|prescription|prescriptions|" +
                "antibiotic|antibiotics|dewormer|wormer|sedative|analgesic|anti-inflammatory|" +
                "medicamento|medicamentos|medicação|medicacao|fármaco|farmaco|" +
                "antibiótico|antibiotico|vermífugo|vermifugo|sedativo|analgésico|analgesico)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val administrationContextRegex =
        Regex(
            "\\b(give|gave|giving|administer|administered|use|inject|apply|" +
                "dar|dá|deu|dar-lhe|administrar|aplicar|injetar)\\b",
            RegexOption.IGNORE_CASE,
        )

    private val nonMedicationQuantityRegex =
        Regex(
            "\\b(how much|how many|quanto|quanta|quantos|quantas)\\b.{0,60}\\b(" +
                "water|feed|food|hay|grass|forage|grain|oats|salt|electrolytes?|" +
                "água|ração|racao|comida|feno|pasto|forragem|cereais|aveia|sal|eletrólitos|" +
                "horse|horses|mare|mares|foal|foals|cavalo|cavalos|égua|éguas|egua|eguas|" +
                "time|tempo|money|cost|custa)\\b",
            RegexOption.IGNORE_CASE,
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
        val hasDosageCue = dosageCueRegex.containsMatchIn(lowered)
        val hasQuantityCue = quantityIntentRegex.containsMatchIn(lowered)
        val hasMeasurementCue = measurementIntentRegex.containsMatchIn(lowered)
        if (hasMeasurementCue || (!hasDosageCue && !hasQuantityCue)) return false

        val hasMedicationCue =
            medicationCueRegex.containsMatchIn(lowered) ||
                RecordTypeIntent
                    .expectedRecordTypes(query)
                    .any { type ->
                        type in
                            setOf(
                                "MEDICATION",
                                "CONTROLLED_SUBSTANCE",
                                "REPRO_MEDICATION",
                                "DEWORMING",
                            )
                    }
        val hasNonMedicationQuantity = nonMedicationQuantityRegex.containsMatchIn(lowered)

        // Check the husbandry object only after detecting medication language:
        // a real request such as "How much metronidazole should I give my
        // horse?" legitimately contains both a drug and an animal.
        if (!hasMedicationCue && hasNonMedicationQuantity) return false

        val hasAdministrationCue = administrationContextRegex.containsMatchIn(lowered)
        return hasDosageCue ||
            (hasMedicationCue && (hasQuantityCue || hasAdministrationCue)) ||
            (hasQuantityCue && hasAdministrationCue && !hasNonMedicationQuantity)
    }
}
