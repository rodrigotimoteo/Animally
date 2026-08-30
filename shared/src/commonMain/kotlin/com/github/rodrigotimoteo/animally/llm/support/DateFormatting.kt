package com.github.rodrigotimoteo.animally.llm.support

import kotlinx.datetime.LocalDate

/**
 * Consolidated human date formatting.
 * Centralizes MONTH_ABBREVIATIONS and formatHumanDate duplicates
 * previously in AnalysisContextBuilder and GenerateRagResponseUseCase.
 */
object DateFormatting {
    val MONTH_ABBREVIATIONS =
        listOf(
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec",
        )

    /**
     * Renders a date as "14 Mar 2026" (locale-independent, model-friendly).
     * Raw ISO strings in prompts leak verbatim into answers — the model parrots
     * exactly what the authoritative block shows.
     */
    fun formatHumanDate(date: LocalDate): String {
        val month = MONTH_ABBREVIATIONS[date.month.ordinal]
        return "${date.day} $month ${date.year}"
    }
}
