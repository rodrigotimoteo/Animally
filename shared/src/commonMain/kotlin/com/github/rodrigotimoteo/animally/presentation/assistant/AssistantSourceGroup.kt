package com.github.rodrigotimoteo.animally.presentation.assistant

import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult

/**
 * Presentation projection for cited records belonging to the same patient.
 *
 * The assistant can cite several records for one horse in a single answer.
 * Keeping the grouping in shared Kotlin prevents the iOS surface from
 * reimplementing source identity rules while retaining one representative
 * source for navigation.
 */
data class AssistantSourceGroup(
    val patientId: Long,
    val patientName: String,
    val primarySource: SearchResult,
    val recordCount: Int,
)

/** Groups cited records by patient while preserving their first-seen order. */
internal fun sourceGroupsForDisplay(sources: List<SearchResult>): List<AssistantSourceGroup> =
    sources
        .groupBy { source ->
            if (source.recordType == OWNER_RECORD_TYPE) {
                "owner:${source.recordId}"
            } else {
                "patient:${source.patientId}"
            }
        }.values
        .map { patientSources ->
            val primary =
                patientSources.firstOrNull { it.recordType == PATIENT_RECORD_TYPE }
                    ?: patientSources.first()
            AssistantSourceGroup(
                patientId = primary.patientId,
                patientName = patientSources.firstNotNullOfOrNull { it.patientName.takeIf(String::isNotBlank) } ?: "",
                primarySource = primary,
                recordCount = patientSources.size,
            )
        }

private const val PATIENT_RECORD_TYPE = "PATIENT"
private const val OWNER_RECORD_TYPE = "OWNER"
