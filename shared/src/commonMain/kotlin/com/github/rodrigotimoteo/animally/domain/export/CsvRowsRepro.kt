package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound

internal val IMAGING_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Type",
        "Date",
        "Findings",
        "ImageUris",
        "VetName",
        "Notes",
    )

internal fun imagingRow(imaging: Imaging): List<Any?> =
    listOf(
        imaging.id,
        imaging.patientId,
        imaging.type,
        imaging.date,
        imaging.findings,
        imaging.imageUris,
        imaging.vetName,
        imaging.notes,
    )

internal val FARRIER_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "TrimOrShoe",
        "ShoeType",
        "Findings",
        "NextDueDate",
        "Farrier",
        "Notes",
    )

internal fun farrierRow(farrierVisit: FarrierVisit): List<Any?> =
    listOf(
        farrierVisit.id,
        farrierVisit.patientId,
        farrierVisit.date,
        farrierVisit.trimOrShoe,
        farrierVisit.shoeType,
        farrierVisit.findings,
        farrierVisit.nextDueDate,
        farrierVisit.farrier,
        farrierVisit.notes,
    )

internal val REPRODUCTION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "EventType",
        "Date",
        "Details",
        "VetName",
        "Notes",
    )

internal fun reproductionEventRow(event: ReproductionEvent): List<Any?> =
    listOf(
        event.id,
        event.patientId,
        event.eventType,
        event.date,
        event.details,
        event.vetName,
        event.notes,
    )

internal val ULTRASOUND_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "OvaryStatus",
        "UterineStatus",
        "FollicleSizeMm",
        "Findings",
        "ImageUris",
        "VetName",
        "Notes",
    )

internal fun ultrasoundRow(ultrasound: Ultrasound): List<Any?> =
    listOf(
        ultrasound.id,
        ultrasound.patientId,
        ultrasound.date,
        ultrasound.ovaryStatus,
        ultrasound.uterineStatus,
        ultrasound.follicleSizeMm,
        ultrasound.findings,
        ultrasound.imageUris,
        ultrasound.vetName,
        ultrasound.notes,
    )

internal val GESTATION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "BreedingDate",
        "ExpectedDueDate",
        "GestationDays",
        "Status",
        "FetalCount",
        "LastCheckDate",
        "Notes",
    )

internal fun gestationRow(gestation: Gestation): List<Any?> =
    listOf(
        gestation.id,
        gestation.patientId,
        gestation.breedingDate,
        gestation.expectedDueDate,
        gestation.gestationDays,
        gestation.status,
        gestation.fetalCount,
        gestation.lastCheckDate,
        gestation.notes,
    )

internal val REPROMEDICATION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Medication",
        "DateAdministered",
        "Dosage",
        "Purpose",
        "VetName",
        "Notes",
    )

internal fun reproMedicationRow(medication: ReproMedication): List<Any?> =
    listOf(
        medication.id,
        medication.patientId,
        medication.medication,
        medication.dateAdministered,
        medication.dosage,
        medication.purpose,
        medication.vetName,
        medication.notes,
    )

internal val CONTROLLED_SUBSTANCE_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "DrugName",
        "Dose",
        "Unit",
        "Route",
        "AdministeredBy",
        "Witness",
        "Date",
        "Reason",
        "Notes",
    )

internal fun controlledSubstanceRow(substance: ControlledSubstance): List<Any?> =
    listOf(
        substance.id,
        substance.patientId,
        substance.drugName,
        substance.dose,
        substance.unit,
        substance.route,
        substance.administeredBy,
        substance.witness,
        substance.date,
        substance.reason,
        substance.notes,
    )
