package com.github.rodrigotimoteo.animally.domain.export

import com.github.rodrigotimoteo.animally.domain.customreminder.model.CustomReminder
import com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer
import com.github.rodrigotimoteo.animally.domain.follicle.model.Follicle
import com.github.rodrigotimoteo.animally.domain.icsi.model.Icsi

internal val CUSTOM_REMINDER_HEADERS =
    listOf("Id", "PatientId", "Title", "DueDate", "LinkedRecordType", "LinkedRecordId", "Notes")

internal fun customReminderRow(reminder: CustomReminder): List<Any?> =
    listOf(
        reminder.id,
        reminder.patientId,
        reminder.title,
        reminder.dueDate,
        reminder.linkedRecordType,
        reminder.linkedRecordId,
        reminder.notes,
    )

internal val EMBRYO_TRANSFER_HEADERS =
    listOf("Id", "PatientId", "Date", "EmbryoCount", "RecipientMares", "Veterinarian", "Notes")

internal fun embryoTransferRow(record: EmbryoTransfer): List<Any?> =
    listOf(
        record.id,
        record.patientId,
        record.date,
        record.embryoCount,
        record.recipientMares,
        record.vetName,
        record.notes,
    )

internal val ICSI_HEADERS =
    listOf("Id", "PatientId", "Date", "FolliclesRecovered", "Veterinarian", "Notes")

internal fun icsiRow(record: Icsi): List<Any?> =
    listOf(
        record.id,
        record.patientId,
        record.date,
        record.folliclesRecovered,
        record.vetName,
        record.notes,
    )

internal val FOLLICLE_HEADERS =
    listOf("Id", "UltrasoundId", "Side", "SizeMm", "Description")

internal fun follicleRow(record: Follicle): List<Any?> =
    listOf(
        record.id,
        record.ultrasoundId,
        record.side,
        record.sizeMm,
        record.description,
    )
