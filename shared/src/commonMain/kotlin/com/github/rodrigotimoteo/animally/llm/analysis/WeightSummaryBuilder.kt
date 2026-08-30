package com.github.rodrigotimoteo.animally.llm.analysis

import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.llm.RagDateRange
import com.github.rodrigotimoteo.animally.llm.support.DateFormatting

internal class WeightSummaryBuilder(
    private val weightRepository: IWeightRepository,
) {
    private data class AnalysisWeightRow(
        val patient: Patient,
        val weight: Weight,
    )

    fun weightTrendBlock(
        targets: List<Patient>,
        dateRange: RagDateRange?,
    ): String? {
        val rows =
            targets
                .flatMap { patient ->
                    weightRepository
                        .getByPatient(patient.id)
                        .filter { weight -> dateRange?.contains(weight.date) != false }
                        .map { weight -> AnalysisWeightRow(patient, weight) }
                }.sortedWith(compareBy({ it.weight.date }, { it.patient.name.lowercase() }, { it.weight.id }))
        if (rows.isEmpty()) return null
        val rowsByPatient = rows.groupBy { it.patient.id }
        if (rowsByPatient.size == 1) {
            return weightTrendLine(rows.first().patient, rows.map(AnalysisWeightRow::weight))
        }

        val values = rows.map { it.weight.weightKg }
        val minimum = rows.minBy { it.weight.weightKg }
        val maximum = rows.maxBy { it.weight.weightKg }
        val details =
            rowsByPatient
                .values
                .sortedBy {
                    it
                        .first()
                        .patient.name
                        .lowercase()
                }.take(MAX_PATIENTS_SCANNED)
                .map { patientRows ->
                    weightTrendLine(patientRows.first().patient, patientRows.map(AnalysisWeightRow::weight))
                }
        val omitted = rowsByPatient.size - details.size
        return buildString {
            appendLine(
                "WEIGHT SUMMARY: ${rows.size} measurements across ${rowsByPatient.size} patients; " +
                    "average ${values.average()} kg, median ${median(values)} kg, " +
                    "minimum ${minimum.weight.weightKg} kg (${minimum.patient.name}, " +
                    "${DateFormatting.formatHumanDate(minimum.weight.date)}), maximum ${maximum.weight.weightKg} kg " +
                    "(${maximum.patient.name}, ${DateFormatting.formatHumanDate(maximum.weight.date)}).",
            )
            appendLine("WEIGHT DETAILS:")
            details.forEach(::appendLine)
            if (omitted > 0) {
                appendLine("- WEIGHT DETAILS TRUNCATED: $omitted more patients are included in the totals above.")
            }
        }.trimEnd()
    }

    private fun weightTrendLine(
        patient: Patient,
        series: List<Weight>,
    ): String {
        val ordered = series.sortedBy(Weight::date)
        val latest = ordered.last()
        if (ordered.size == 1) {
            return "- Weight ${patient.name}: single measurement ${latest.weightKg} kg " +
                "on ${DateFormatting.formatHumanDate(latest.date)}."
        }
        val previous = ordered[ordered.lastIndex - 1]
        val min = ordered.minBy(Weight::weightKg)
        val max = ordered.maxBy(Weight::weightKg)
        val direction = weightDirection(latest.weightKg, previous.weightKg)
        return "- Weight ${patient.name}: min ${min.weightKg} kg (${min.date}), max ${max.weightKg} kg " +
            "(${max.date}), latest ${latest.weightKg} kg (${latest.date}) - $direction."
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun weightDirection(
        latestKg: Double,
        previousKg: Double,
    ): String =
        when {
            latestKg > previousKg + STABLE_WEIGHT_DELTA_KG -> "gaining"
            latestKg < previousKg - STABLE_WEIGHT_DELTA_KG -> "losing"
            else -> "stable"
        }
}
