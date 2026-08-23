package com.github.rodrigotimoteo.animally.domain.care

import kotlinx.datetime.LocalDate

/**
 * One upcoming or overdue care item surfaced on the patient Overview tab.
 *
 * @property typeLabel The kind of care, e.g. "Vaccination", "Dentistry", "Farrier".
 * @property title Short human-readable summary, e.g. the vaccine name.
 * @property dueDate The date the care is due. May be in the past when [overdue].
 * @property overdue Whether [dueDate] is before today.
 */
data class CareDueItem(
    val typeLabel: String,
    val title: String,
    val dueDate: LocalDate,
    val overdue: Boolean,
)
