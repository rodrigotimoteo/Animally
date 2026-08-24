package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.presentation.dentistry.DentistryFormState
import com.github.rodrigotimoteo.animally.presentation.deworming.DewormingFormState
import com.github.rodrigotimoteo.animally.presentation.farrier.FarrierVisitFormState
import com.github.rodrigotimoteo.animally.presentation.vaccination.VaccinationFormState

/**
 * Field-row builders for the preventive-care record types of the read-only
 * detail. Labels and ordering mirror the tab views' preview rows exactly.
 */
internal fun vaccinationRows(form: VaccinationFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date Administered" to form.dateAdministered,
            "Vaccine" to form.vaccineName,
            "Batch Number" to form.batchNumber,
            "Site" to form.site,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun dewormingRows(form: DewormingFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Product" to form.product,
            "Dose" to form.dose,
            "Date Administered" to form.dateAdministered,
            "Next Due" to form.nextDueDate,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun dentistryRows(form: DentistryFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Treatment" to form.treatment,
            "Findings" to form.findings,
            "Next Due" to form.nextDueDate,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun farrierRows(form: FarrierVisitFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Trim or Shoe" to form.trimOrShoe,
            "Shoe Type" to form.shoeType,
            "Findings" to form.findings,
            "Next Due" to form.nextDueDate,
            "Farrier" to form.farrier,
            "Notes" to form.notes,
        ),
    )
