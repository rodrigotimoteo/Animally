package com.github.rodrigotimoteo.animally.presentation.ios

import com.github.rodrigotimoteo.animally.presentation.anamnese.AnamneseFormState
import com.github.rodrigotimoteo.animally.presentation.customreminder.CustomReminderFormState

/** Field-row builders for records that are not part of a multi-row iOS tab yet. */
internal fun anamneseRows(form: AnamneseFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "General History" to form.generalHistory,
            "Chronic Conditions" to form.chronicConditions,
            "Allergies" to form.allergies,
        ),
    )

internal fun customReminderRows(form: CustomReminderFormState): List<RecordDetailRow> =
    recordDetailRows(
        listOf(
            "Title" to form.title,
            "Due Date" to form.dueDate,
            "Linked Record Type" to form.linkedRecordType,
            "Linked Record ID" to form.linkedRecordId,
            "Notes" to form.notes,
        ),
    )
