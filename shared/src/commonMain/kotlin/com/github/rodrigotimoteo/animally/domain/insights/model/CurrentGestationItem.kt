package com.github.rodrigotimoteo.animally.domain.insights.model

import kotlinx.datetime.LocalDate

/**
 * One active gestation row in the current operational snapshot.
 *
 * `gestationDay` is recalculated from `breedingDate` and the injected `today`,
 * not trusted from stored `gestationDays`. The due date remains the persisted
 * `expectedDueDate`, so explicitly adjusted clinical dates are respected.
 * `daysUntilDue` may be negative for overdue rows, making that state explicit.
 *
 * @property patientId mare identifier.
 * @property patientName mare display name.
 * @property gestationId source gestation row identifier for navigation.
 * @property gestationDay days since breeding as of the captured today.
 * @property dueDate expected foaling date.
 * @property daysUntilDue days from today to [dueDate]; negative when overdue.
 * @property status gestation status label as stored.
 */
data class CurrentGestationItem(
    val patientId: Long,
    val patientName: String,
    val gestationId: Long,
    val gestationDay: Int,
    val dueDate: LocalDate,
    val daysUntilDue: Int,
    val status: String,
) {
    fun isDueSoon(days: Int): Boolean = daysUntilDue in 0..days
}
