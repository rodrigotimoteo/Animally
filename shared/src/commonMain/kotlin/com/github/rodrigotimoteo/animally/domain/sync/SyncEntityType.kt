package com.github.rodrigotimoteo.animally.domain.sync

import kotlinx.serialization.Serializable

/**
 * Wire names of every syncable entity type.
 *
 * The [wireName] values are stable API surface — they are what travels in
 * [SyncRecord.type] and must never be renamed. This lane only maps names; it
 * holds no knowledge of the entities themselves.
 */
@Serializable
enum class SyncEntityType(
    val wireName: String,
) {
    OWNER("Owner"),
    PATIENT("Patient"),
    ANAMNESE("Anamnese"),
    CONSULTATION("Consultation"),
    DENTISTRY("Dentistry"),
    DEWORMING("Deworming"),
    FARRIER_VISIT("FarrierVisit"),
    GESTATION("Gestation"),
    IMAGING("Imaging"),
    LAB_RESULT("LabResult"),
    LAMENESS("Lameness"),
    MEDICATION("Medication"),
    REPRODUCTION("Reproduction"),
    REPRO_MEDICATION("ReproMedication"),
    SUBSTANCE("Substance"),
    SURGERY("Surgery"),
    ULTRASOUND("Ultrasound"),
    VACCINATION("Vaccination"),
    WEIGHT("Weight"),
    CUSTOM_REMINDER("CustomReminder"),
    ;

    companion object {
        fun fromWireName(name: String): SyncEntityType? = entries.firstOrNull { it.wireName == name }
    }
}
