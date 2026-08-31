package com.github.rodrigotimoteo.animally.domain.reproduction.model

/**
 * Canonical taxonomy for reproduction events.
 *
 * Each entry carries a stable [storageLabel] persisted in the database and a [displayLabel]
 * shown in UI. Both labels are identical for the current vocabulary so picker values
 * round-trip through storage without transformation.
 *
 * Parsing is tolerant to legacy spellings: case-insensitive and ignores spaces,
 * underscores and hyphens. Unknown values map to [Other] while the raw string
 * in [ReproductionEvent.eventType] remains untouched (no migration of historical rows).
 */
enum class ReproductionEventType(
    val storageLabel: String,
    val displayLabel: String,
) {
    Heat("Heat", "Heat"),
    Breeding("Breeding", "Breeding"),
    PregnancyCheck("Pregnancy Check", "Pregnancy Check"),
    Foaling("Foaling", "Foaling"),
    InitialExam("Initial Exam", "Initial Exam"),
    Other("Other", "Other"),
    ;

    companion object {
        fun normalize(value: String): String = value.filterNot { it == ' ' || it == '_' || it == '-' }.lowercase()

        /** Known picker values that round-trip through [from]. */
        val knownEntries: List<ReproductionEventType> = entries.filter { it != Other }

        private val byNormalized: Map<String, ReproductionEventType> =
            knownEntries.associateBy { normalize(it.storageLabel) }

        /** Normalized known labels for SQL NOT IN single-source derivation. */
        val knownNormalizedValues: List<String> = knownEntries.map { normalize(it.storageLabel) }

        /**
         * Parses [raw] into a canonical type.
         * Tolerant to case, spaces, underscores and hyphens.
         * Returns [Other] for unknown values.
         */
        fun from(raw: String): ReproductionEventType = byNormalized[normalize(raw.trim())] ?: Other
    }
}
