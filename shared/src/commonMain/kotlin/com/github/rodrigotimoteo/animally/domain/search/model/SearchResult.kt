package com.github.rodrigotimoteo.animally.domain.search.model

import kotlinx.datetime.LocalDate

/**
 * A single hit from the global search index.
 *
 * @property patientId Identifier of the patient the hit belongs to. Owner hits
 * have no patient relationship and retain their legacy owner id here for
 * source compatibility; use [ownerId] to identify those hits.
 * @property ownerId Identifier of the owner for an owner hit, otherwise `null`.
 * @property patientName Name of the patient.
 * @property breed Optional breed of the patient.
 * @property microchipId Optional microchip identifier of the patient.
 * @property recordType Type of the indexed record (e.g. `PATIENT`, `CONSULTATION`, `MEDICATION`).
 * @property recordId Identifier of the indexed record within its own table.
 * @property date Optional date associated with the record (e.g. consultation date).
 * @property snippet The indexed searchable text that matched.
 */
data class SearchResult(
    val patientId: Long,
    val patientName: String,
    val breed: String?,
    val microchipId: String?,
    val recordType: String,
    val recordId: Long,
    val date: LocalDate?,
    val snippet: String,
    val ownerId: Long? = null,
)
