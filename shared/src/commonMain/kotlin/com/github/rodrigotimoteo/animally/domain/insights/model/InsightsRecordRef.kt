package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import kotlinx.datetime.LocalDate

/**
 * Reference to one source record surfaced by drill-down.
 *
 * @property recordType source record type discriminator.
 * @property patientId owning patient identifier.
 * @property recordId source row identifier.
 * @property patientName owning patient display name for list rendering.
 * @property date record date used for ordering and filtering.
 */
data class InsightsRecordRef(
    val recordType: RecordType,
    val patientId: Long,
    val recordId: Long,
    val patientName: String,
    val date: LocalDate,
)
