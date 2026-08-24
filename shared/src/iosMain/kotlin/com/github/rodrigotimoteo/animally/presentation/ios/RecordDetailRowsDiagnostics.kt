package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.presentation.imaging.ImagingFormState
import com.github.rodrigotimoteo.animally.presentation.labresult.LabResultFormState

/**
 * Field-row builders for the diagnostics record types of the read-only
 * detail. Labels and ordering mirror the tab views' preview rows exactly.
 */
internal fun labResultRows(form: LabResultFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Test Type" to form.testType,
            "Results" to form.results,
            "Normal Range" to form.normalRange,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )

internal fun imagingRows(form: ImagingFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Date" to form.date,
            "Type" to form.type,
            "Findings" to form.findings,
            "Veterinarian" to form.vetName,
            "Notes" to form.notes,
        ),
    )
