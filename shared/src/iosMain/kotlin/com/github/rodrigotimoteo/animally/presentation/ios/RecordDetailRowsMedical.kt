package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.presentation.consultation.ConsultationFormState
import com.github.rodrigotimoteo.animally.presentation.lameness.LamenessFormState
import com.github.rodrigotimoteo.animally.presentation.medication.MedicationFormState
import com.github.rodrigotimoteo.animally.presentation.substance.ControlledSubstanceFormState
import com.github.rodrigotimoteo.animally.presentation.surgery.SurgeryFormState
import com.github.rodrigotimoteo.animally.presentation.weight.WeightFormState

/**
 * Field-row builders for the medical-group record types of the read-only
 * detail. Labels and ordering mirror the tab views' preview rows exactly.
 */
internal fun consultationRows(form: ConsultationFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Subjective" to form.subjective,
            "Objective" to form.objective,
            "Assessment" to form.assessment,
            "Plan" to form.plan,
            "Veterinarian" to form.vetName,
            "Next Visit" to form.nextVisitDate,
        ),
    )

internal fun lamenessRows(form: LamenessFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "AAEP Grade" to form.gradeAAEP,
            "Limb Location" to form.limbLocation,
            "Flexion Test" to form.flexionTest,
            "Diagnosis" to form.diagnosis,
            "Treatment" to form.treatment,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun surgeryRows(form: SurgeryFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Type" to form.type,
            "Description" to form.description,
            "Outcome" to form.outcome,
            "Surgeon" to form.surgeon,
            "Anesthesia" to form.anesthesia,
            "Analgesia" to form.analgesia,
            "Complications" to form.complications,
            "Recovery Notes" to form.recoveryNotes,
        ),
    )

internal fun medicationRows(form: MedicationFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Name" to form.name,
            "Dosage" to form.dosage,
            "Route" to form.route,
            "Frequency" to form.frequency,
            "Start Date" to form.startDate,
            "End Date" to form.endDate,
            "Prescribed By" to form.prescribedBy,
            "Notes" to form.notes,
        ),
    )

internal fun substanceRows(form: ControlledSubstanceFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Drug Name" to form.drugName,
            "Dose" to form.dose,
            "Unit" to form.unit,
            "Route" to form.route,
            "Date" to form.date,
            "Administered By" to form.administeredBy,
            "Witness" to form.witness,
            "Reason" to form.reason,
            "Notes" to form.notes,
        ),
    )

internal fun weightRows(form: WeightFormState): List<RecordDetailRow> {
    val weight = form.weightKg.toDoubleOrNull()?.let { "${formatOneDecimal(it)} kg" } ?: ""
    return recordDetailRows(
        listOf(
            "Date" to form.date,
            "Weight (kg)" to weight,
            "Notes" to form.notes,
        ),
    )
}
