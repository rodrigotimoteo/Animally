package com.github.rodrigotimoteo.animally.domain.common

/**
 * Every record type that participates in cross-cutting string-keyed flows:
 * timeline entries, the FTS search index, and record deep-link routing.
 *
 * Two stable string vocabularies exist and both cross the Kotlin/Swift bridge,
 * so their values must never change:
 * - [wireName]: uppercase identifier stored in the FTS index and carried by
 *   [com.github.rodrigotimoteo.animally.domain.search.model.SearchResult.recordType].
 * - [displayName]: human-readable label carried by
 *   [com.github.rodrigotimoteo.animally.domain.timeline.model.TimelineEntry.recordType]
 *   and used for routing back to the source record editor.
 *
 * Prefer parsing strings back to this enum ([fromWireName] / [fromDisplayName])
 * over comparing raw strings at dispatch sites.
 */
enum class RecordType(
    val wireName: String,
    val displayName: String,
) {
    Consultation("CONSULTATION", "Consultation"),
    Vaccination("VACCINATION", "Vaccination"),
    Deworming("DEWORMING", "Deworming"),
    Dentistry("DENTISTRY", "Dentistry"),
    FarrierVisit("FARRIER_VISIT", "Farrier"),
    Lameness("LAMENESS", "Lameness"),
    Surgery("SURGERY", "Surgery"),
    Medication("MEDICATION", "Medication"),
    ControlledSubstance("CONTROLLED_SUBSTANCE", "Controlled Substance"),
    Weight("WEIGHT", "Weight"),
    Anamnese("ANAMNESE", "Anamnese"),
    ReproductionEvent("REPRODUCTION_EVENT", "Reproduction"),
    Ultrasound("ULTRASOUND", "Ultrasound"),
    Gestation("GESTATION", "Gestation"),
    ReproMedication("REPRO_MEDICATION", "Repro Medication"),
    LabResult("LAB_RESULT", "Lab Result"),
    Imaging("IMAGING", "Imaging"),
    CustomReminder("CUSTOM_REMINDER", "Custom Reminder"),
    EmbryoTransfer("EMBRYO_TRANSFER", "Embryo Transfer"),
    Icsi("ICSI", "Icsi"),
    Owner("OWNER", "Owner"),
    Patient("PATIENT", "Patient"),
    ;

    companion object {
        private val byWireName = entries.associateBy { it.wireName }
        private val byDisplayName = entries.associateBy { it.displayName }

        /** Parses a wire name (FTS index vocabulary); null when unknown. */
        fun fromWireName(wireName: String): RecordType? = byWireName[wireName]

        /** Parses a display name (timeline vocabulary); null when unknown. */
        fun fromDisplayName(displayName: String): RecordType? = byDisplayName[displayName]
    }
}
