package com.github.rodrigotimoteo.animally.domain.insights.usecase

private const val PSEUDONYM_PAD = 3
private const val FALLBACK_PSEUDONYM = "P000"

internal fun insightsPseudonym(index: Int): String = "P${(index + 1).toString().padStart(PSEUDONYM_PAD, '0')}"

internal fun insightsPatientColumns(pseudonymize: Boolean): List<String> =
    if (pseudonymize) {
        listOf("patient_pseudonym")
    } else {
        listOf("patient_id", "patient_name")
    }

internal fun insightsPatientCells(
    pseudonymize: Boolean,
    patientId: Long,
    patientName: String,
    pseudonymMap: Map<Long, String>,
): List<String> =
    if (pseudonymize) {
        listOf(pseudonymMap[patientId] ?: FALLBACK_PSEUDONYM)
    } else {
        listOf(patientId.toString(), patientName)
    }

internal fun formatInsightsDouble(value: Double?): String = value?.toString() ?: ""

internal fun insightsFlag(value: Boolean): String = if (value) "1" else "0"
