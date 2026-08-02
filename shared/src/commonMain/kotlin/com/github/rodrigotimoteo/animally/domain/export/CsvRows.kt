package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight

internal val PATIENT_HEADERS =
    listOf(
        "Id",
        "Name",
        "Species",
        "Breed",
        "DateOfBirth",
        "Gender",
        "MicrochipId",
        "UELN",
        "RegistrationNumber",
        "StableLocation",
        "Notes",
        "OwnerId",
        "Active",
    )

internal fun patientRow(patient: Patient): List<Any?> =
    listOf(
        patient.id,
        patient.name,
        patient.species,
        patient.breed,
        patient.dateOfBirth,
        patient.gender,
        patient.microchipId,
        patient.ueln,
        patient.registrationNumber,
        patient.stableLocation,
        patient.notes,
        patient.ownerId,
        patient.isActive,
    )

internal val ANAMNESE_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "GeneralHistory",
        "ChronicConditions",
        "Allergies",
    )

internal fun anamneseRow(anamnese: Anamnese): List<Any?> =
    listOf(
        anamnese.id,
        anamnese.patientId,
        anamnese.generalHistory,
        anamnese.chronicConditions,
        anamnese.allergies,
    )

internal val WEIGHT_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "WeightKg",
        "Date",
        "Notes",
    )

internal fun weightRow(weight: Weight): List<Any?> =
    listOf(
        weight.id,
        weight.patientId,
        weight.weightKg,
        weight.date,
        weight.notes,
    )

internal val CONSULTATION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "Subjective",
        "Objective",
        "Assessment",
        "Plan",
        "VetName",
        "NextVisitDate",
    )

internal fun consultationRow(consultation: Consultation): List<Any?> =
    listOf(
        consultation.id,
        consultation.patientId,
        consultation.date,
        consultation.subjective,
        consultation.objective,
        consultation.assessment,
        consultation.plan,
        consultation.vetName,
        consultation.nextVisitDate,
    )

internal val VACCINATION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "VaccineName",
        "DateAdministered",
        "NextDueDate",
        "VetName",
        "BatchNumber",
        "Site",
        "Notes",
    )

internal fun vaccinationRow(vaccination: Vaccination): List<Any?> =
    listOf(
        vaccination.id,
        vaccination.patientId,
        vaccination.vaccineName,
        vaccination.dateAdministered,
        vaccination.nextDueDate,
        vaccination.vetName,
        vaccination.batchNumber,
        vaccination.site,
        vaccination.notes,
    )

internal val DEWORMING_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Product",
        "DateAdministered",
        "NextDueDate",
        "Dose",
        "VetName",
        "Notes",
    )

internal fun dewormingRow(deworming: Deworming): List<Any?> =
    listOf(
        deworming.id,
        deworming.patientId,
        deworming.product,
        deworming.dateAdministered,
        deworming.nextDueDate,
        deworming.dose,
        deworming.vetName,
        deworming.notes,
    )

internal val DENTISTRY_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "Findings",
        "Treatment",
        "NextDueDate",
        "VetName",
        "Notes",
    )

internal fun dentistryRow(dentistry: Dentistry): List<Any?> =
    listOf(
        dentistry.id,
        dentistry.patientId,
        dentistry.date,
        dentistry.findings,
        dentistry.treatment,
        dentistry.nextDueDate,
        dentistry.vetName,
        dentistry.notes,
    )

internal val LAMENESS_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "GradeAAEP",
        "LimbLocation",
        "FlexionTest",
        "Diagnosis",
        "Treatment",
        "VetName",
        "Notes",
    )

internal fun lamenessRow(lameness: Lameness): List<Any?> =
    listOf(
        lameness.id,
        lameness.patientId,
        lameness.date,
        lameness.gradeAAEP,
        lameness.limbLocation,
        lameness.flexionTest,
        lameness.diagnosis,
        lameness.treatment,
        lameness.vetName,
        lameness.notes,
    )

internal val SURGERY_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Date",
        "Type",
        "Description",
        "Outcome",
        "Surgeon",
        "Anesthesia",
        "Analgesia",
        "Complications",
        "RecoveryNotes",
    )

internal fun surgeryRow(surgery: Surgery): List<Any?> =
    listOf(
        surgery.id,
        surgery.patientId,
        surgery.date,
        surgery.type,
        surgery.description,
        surgery.outcome,
        surgery.surgeon,
        surgery.anesthesia,
        surgery.analgesia,
        surgery.complications,
        surgery.recoveryNotes,
    )

internal val MEDICATION_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "Name",
        "Dosage",
        "Route",
        "Frequency",
        "StartDate",
        "EndDate",
        "PrescribedBy",
        "Notes",
    )

internal fun medicationRow(medication: Medication): List<Any?> =
    listOf(
        medication.id,
        medication.patientId,
        medication.name,
        medication.dosage,
        medication.route,
        medication.frequency,
        medication.startDate,
        medication.endDate,
        medication.prescribedBy,
        medication.notes,
    )

internal val LABRESULT_HEADERS =
    listOf(
        "Id",
        "PatientId",
        "TestType",
        "Date",
        "Results",
        "NormalRange",
        "VetName",
        "Notes",
    )

internal fun labResultRow(labResult: LabResult): List<Any?> =
    listOf(
        labResult.id,
        labResult.patientId,
        labResult.testType,
        labResult.date,
        labResult.results,
        labResult.normalRange,
        labResult.vetName,
        labResult.notes,
    )
