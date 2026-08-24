package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.presentation.embryotransfer.EmbryoTransferFormState
import com.github.rodrigotimoteo.animally.presentation.gestation.GestationFormState
import com.github.rodrigotimoteo.animally.presentation.icsi.IcsiFormState
import com.github.rodrigotimoteo.animally.presentation.reproduction.ReproductionEventFormState
import com.github.rodrigotimoteo.animally.presentation.repromedication.ReproMedicationFormState
import com.github.rodrigotimoteo.animally.presentation.ultrasound.FollicleRow
import com.github.rodrigotimoteo.animally.presentation.ultrasound.UltrasoundFormState

/**
 * Field-row builders for the reproduction record types of the read-only
 * detail. Labels and ordering mirror the tab views' preview rows exactly.
 */
internal fun reproductionRows(form: ReproductionEventFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Event Type" to form.eventType,
            "Details" to form.details,
            "Initial Exam Findings" to form.initialExamFindings,
            "Stallion" to form.stallionName,
            "Breeding Type" to form.breedingType,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun ultrasoundRows(form: UltrasoundFormState): List<RecordDetailRow> {
    val fields =
        mutableListOf(
            RecordDetailRow(label = "Date", value = form.date),
            RecordDetailRow(label = "Ovary Status", value = form.ovaryStatus ?: ""),
            RecordDetailRow(label = "Uterine Status", value = form.uterineStatus ?: ""),
            RecordDetailRow(label = "Left Ovary Status", value = form.leftOvaryStatus ?: ""),
        )
    val leftSummary = follicleSummary(form.leftFollicles)
    if (leftSummary.isNotEmpty()) {
        fields.add(RecordDetailRow(label = "Left Follicles", value = leftSummary))
    }
    fields.add(RecordDetailRow(label = "Right Ovary Status", value = form.rightOvaryStatus ?: ""))
    val rightSummary = follicleSummary(form.rightFollicles)
    if (rightSummary.isNotEmpty()) {
        fields.add(RecordDetailRow(label = "Right Follicles", value = rightSummary))
    }
    fields.add(RecordDetailRow(label = "Uterine Edema", value = form.uterineEdema ?: ""))
    if (form.uterineLiquid == true) {
        form.uterineLiquidDescription?.let {
            fields.add(RecordDetailRow(label = "Fluid Description", value = it))
        }
    }
    fields.add(RecordDetailRow(label = "Uterus Description", value = form.uterusDescription ?: ""))
    fields.add(RecordDetailRow(label = "Findings", value = form.findings ?: ""))
    fields.add(RecordDetailRow(label = "Veterinarian", value = form.vetName ?: ""))
    fields.add(RecordDetailRow(label = "Notes", value = form.notes ?: ""))
    return fields.filter { it.value.isNotEmpty() }
}

/**
 * Renders recorded follicles as one compact line: "12.5 mm, 9 mm — note".
 */
private fun follicleSummary(follicles: List<FollicleRow>): String =
    follicles
        .mapNotNull { follicle ->
            val size = follicle.sizeMm.trim()
            if (size.isEmpty()) return@mapNotNull null
            val note = follicle.note
            if (!note.isNullOrEmpty()) "$size mm — $note" else "$size mm"
        }.joinToString(separator = ", ")

internal fun gestationRows(form: GestationFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Breeding Date" to form.breedingDate,
            "Status" to form.status,
            "Fetal Count" to form.fetalCount,
            "Last Check Date" to form.lastCheckDate,
            "Notes" to form.notes,
        ),
    )

internal fun reproMedicationRows(form: ReproMedicationFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Medication" to form.medication,
            "Date Administered" to form.dateAdministered,
            "Dosage" to form.dosage,
            "Purpose" to form.purpose,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun embryoTransferRows(form: EmbryoTransferFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Embryo Count" to form.embryoCount,
            "Recipient Mares" to form.recipientMares,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun icsiRows(form: IcsiFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Follicles Recovered" to form.folliclesRecovered,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )
