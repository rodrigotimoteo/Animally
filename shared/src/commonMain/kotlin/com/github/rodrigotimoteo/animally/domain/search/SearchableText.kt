package com.github.rodrigotimoteo.animally.domain.search

import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination

/**
 * Canonical searchable text for records that cross the save and re-index paths.
 * Keeping field selection here prevents an edit from disappearing from search
 * until the next healing pass.
 */
@Suppress("TooManyFunctions")
internal object SearchableText {
    fun patient(patient: Patient): String =
        listOfNotNull(
            patient.name,
            patient.species,
            patient.breed,
            patient.microchipId,
            patient.ueln,
            patient.registrationNumber,
            patient.stableLocation,
            patient.notes,
        ).joinToString(" ")

    fun consultation(consultation: Consultation): String =
        listOfNotNull(
            consultation.subjective,
            consultation.objective,
            consultation.assessment,
            consultation.plan,
            consultation.vetName,
        ).joinToString(" ")

    fun medication(medication: Medication): String =
        listOfNotNull(
            medication.name,
            medication.dosage,
        ).joinToString(" ")

    fun vaccination(vaccination: Vaccination): String =
        listOfNotNull(
            vaccination.vaccineName,
            vaccination.batchNumber,
            vaccination.vetName,
            vaccination.site,
            vaccination.notes,
            "vaccination vaccine booster shot",
        ).joinToString(" ")

    fun farrierVisit(visit: FarrierVisit): String =
        listOfNotNull(
            visit.trimOrShoe,
            visit.shoeType,
            visit.findings,
            visit.farrier,
            visit.notes,
            "farrier visit trim shoeing care",
        ).joinToString(" ")

    fun lameness(lameness: Lameness): String =
        listOfNotNull(
            lameness.gradeAAEP.toString(),
            lameness.limbLocation,
            lameness.flexionTest,
            lameness.diagnosis,
            lameness.treatment,
            lameness.vetName,
            lameness.notes,
            "grade flexion",
        ).joinToString(" ")

    fun surgery(surgery: Surgery): String =
        listOfNotNull(
            surgery.type,
            surgery.description,
            surgery.outcome,
            surgery.surgeon,
            surgery.anesthesia,
            surgery.analgesia,
            surgery.complications,
            surgery.recoveryNotes,
            "surgeon",
        ).joinToString(" ")

    fun controlledSubstance(record: ControlledSubstance): String =
        listOfNotNull(
            record.drugName,
            record.dose,
            record.unit,
            record.route,
            record.administeredBy,
            record.witness,
            record.reason,
            record.notes,
            "witness",
        ).joinToString(" ")

    fun gestation(gestation: Gestation): String {
        val isResolved =
            gestation.status.equals("Completed", ignoreCase = true) ||
                gestation.status.equals("Failed", ignoreCase = true) ||
                gestation.status.equals("Foaled", ignoreCase = true)
        val pregnancyVocabulary =
            if (isResolved) null else "pregnant in foal active gestation expected foaling"
        return listOfNotNull(
            gestation.breedingDate.toString(),
            gestation.status,
            gestation.notes,
            pregnancyVocabulary,
        ).joinToString(" ")
    }

    fun anamnese(anamnese: Anamnese): String =
        listOfNotNull(
            anamnese.generalHistory,
            anamnese.chronicConditions,
            anamnese.allergies,
            "anamnese medical history",
        ).joinToString(" ")

    fun customReminder(reminder: CustomReminder): String =
        listOfNotNull(
            reminder.title,
            reminder.linkedRecordType,
            reminder.notes,
            "reminder",
        ).joinToString(" ")

    fun embryoTransfer(record: EmbryoTransfer): String =
        listOfNotNull(
            record.embryoCount.toString(),
            record.recipientMares,
            record.vetName,
            record.notes,
            "embryo transfer flush donor recipient",
        ).joinToString(" ")

    fun icsi(record: Icsi): String =
        listOfNotNull(
            record.folliclesRecovered.toString(),
            record.vetName,
            record.notes,
            // Keep the record-type vocabulary, but do not inject the generic
            // "follicle" token: that would make every ICSI record leak into
            // broad follicle searches even when its note contains no follicle
            // finding. The numeric count and actual notes remain searchable.
            "icsi",
        ).joinToString(" ")
}
