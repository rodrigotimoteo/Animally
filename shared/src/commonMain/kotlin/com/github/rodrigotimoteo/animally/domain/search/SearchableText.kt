package com.github.rodrigotimoteo.animally.domain.search

import com.github.rodrigotimoteo.animally.domain.anamnese.model.Anamnese
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.dentistry.model.Dentistry
import com.github.rodrigotimoteo.animally.domain.deworming.model.Deworming
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.farrier.model.FarrierVisit
import com.github.rodrigotimoteo.animally.domain.gestation.model.Gestation
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi
import com.github.rodrigotimoteo.animally.domain.imaging.model.Imaging
import com.github.rodrigotimoteo.animally.domain.labresult.model.LabResult
import com.github.rodrigotimoteo.animally.domain.lameness.model.Lameness
import com.github.rodrigotimoteo.animally.domain.medication.model.Medication
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEvent
import com.github.rodrigotimoteo.animally.domain.repromedication.model.ReproMedication
import com.github.rodrigotimoteo.animally.domain.substance.model.ControlledSubstance
import com.github.rodrigotimoteo.animally.domain.surgery.model.Surgery
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight

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
            patient.dateOfBirth?.let { "date of birth $it" },
            patient.gender?.let { "gender $it" },
            patient.microchipId,
            patient.ueln,
            patient.registrationNumber,
            patient.stableLocation,
            patient.notes,
            patient.cogginsTestDate?.let { "coggins test date $it" },
            patient.cogginsResult?.let { "coggins result $it" },
            patient.cogginsExpiryDate?.let { "coggins expiry date $it" },
        ).joinToString(" ")

    fun owner(owner: Owner): String =
        listOfNotNull(
            owner.name,
            owner.email,
            owner.phone,
            owner.address,
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
            medication.route,
            medication.frequency,
            medication.startDate?.let { "start date $it" },
            medication.endDate?.let { "end date $it" },
            medication.prescribedBy,
            medication.notes,
            "medication medicine drug prescription treatment",
        ).joinToString(" ")

    fun vaccination(vaccination: Vaccination): String =
        listOfNotNull(
            vaccination.vaccineName,
            vaccination.batchNumber,
            vaccination.vetName,
            vaccination.site,
            vaccination.notes,
            vaccination.nextDueDate?.let { "next due $it" },
            "vaccination vaccine booster shot",
        ).joinToString(" ")

    fun farrierVisit(visit: FarrierVisit): String =
        listOfNotNull(
            visit.trimOrShoe,
            visit.shoeType,
            visit.findings,
            visit.farrier,
            visit.notes,
            visit.nextDueDate?.let { "next due $it" },
            "farrier visit trim shoeing care",
        ).joinToString(" ")

    fun deworming(record: Deworming): String =
        listOfNotNull(
            record.product,
            record.dose,
            record.nextDueDate?.let { "next due $it" },
            record.vetName,
            record.notes,
            "deworming dewormer wormer anthelmintic",
        ).joinToString(" ")

    fun dentistry(record: Dentistry): String =
        listOfNotNull(
            record.findings,
            record.treatment,
            record.nextDueDate?.let { "next dental check $it" },
            record.vetName,
            record.notes,
            "dentistry dental tooth teeth oral",
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

    fun ultrasound(ultrasound: Ultrasound): String =
        listOfNotNull(
            ultrasound.ovaryStatus,
            ultrasound.uterineStatus,
            ultrasound.follicleSizeMm?.toString(),
            ultrasound.leftOvaryStatus,
            ultrasound.rightOvaryStatus,
            ultrasound.leftFollicleSizeMm?.toString(),
            ultrasound.rightFollicleSizeMm?.toString(),
            ultrasound.uterineEdema,
            ultrasound.uterineLiquid?.toString(),
            ultrasound.uterineLiquidDescription,
            ultrasound.uterusDescription,
            ultrasound.findings,
            ultrasound.vetName,
            ultrasound.notes,
            "ultrasound ecography ultrasonography findings examination",
        ).joinToString(" ")

    fun gestation(gestation: Gestation): String {
        val isResolved =
            gestation.status.equals("Completed", ignoreCase = true) ||
                gestation.status.equals("Failed", ignoreCase = true) ||
                gestation.status.equals("Foaled", ignoreCase = true)
        val pregnancyVocabulary =
            if (isResolved) null else "pregnant in foal active gestation expected foaling"
        val dueDateLabel =
            if (isResolved) {
                "due ${gestation.expectedDueDate}"
            } else {
                "expected foaling ${gestation.expectedDueDate}"
            }
        val gestationDayLabel =
            if (isResolved) null else "gestation day ${gestation.gestationDays}"
        return listOfNotNull(
            gestation.breedingDate.toString(),
            dueDateLabel,
            gestationDayLabel,
            gestation.status,
            gestation.fetalCount?.let { "fetal count $it" },
            gestation.lastCheckDate?.let { "last pregnancy check $it" },
            gestation.notes,
            pregnancyVocabulary,
        ).joinToString(" ")
    }

    fun anamnese(anamnese: Anamnese): String =
        listOfNotNull(
            "general history: ${anamnese.generalHistory}",
            "chronic conditions: ${anamnese.chronicConditions}",
            "allergies: ${anamnese.allergies}",
            "anamnese medical history",
        ).joinToString(" ")

    fun customReminder(reminder: CustomReminder): String =
        listOfNotNull(
            reminder.title,
            "due ${reminder.dueDate}",
            reminder.linkedRecordType,
            reminder.notes,
            "reminder",
        ).joinToString(" ")

    fun weight(record: Weight): String =
        listOfNotNull(
            record.weightKg.toString(),
            record.notes,
            "kg weight measurement bodyweight",
        ).joinToString(" ")

    fun reproductionEvent(record: ReproductionEvent): String =
        listOfNotNull(
            record.eventType,
            record.details,
            record.initialExamFindings,
            record.stallionName,
            record.breedingType,
            record.vetName,
            record.notes,
            "reproduction reproductive event",
        ).joinToString(" ")

    fun reproMedication(record: ReproMedication): String =
        listOfNotNull(
            record.medication,
            record.dosage,
            record.purpose,
            record.vetName,
            record.notes,
            "reproductive medication repro medication",
        ).joinToString(" ")

    fun labResult(record: LabResult): String {
        val bloodTestVocabulary =
            if (record.testType.containsAnyIgnoreCase("blood", "cbc", "hematology", "haematology")) {
                "bloodwork blood test"
            } else {
                null
            }
        return listOfNotNull(
            record.testType,
            record.results,
            record.normalRange,
            record.vetName,
            record.notes,
            bloodTestVocabulary,
            "lab laboratory result",
        ).joinToString(" ")
    }

    fun imaging(record: Imaging): String =
        listOfNotNull(
            record.type,
            record.findings,
            record.imageUris,
            record.vetName,
            record.notes,
            "imaging image diagnostic study",
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

private fun String.containsAnyIgnoreCase(vararg values: String): Boolean =
    values.any { value ->
        contains(value, ignoreCase = true)
    }
