package com.github.rodrigotimoteo.animally.domain.insights.model

/**
 * Reproduction activity totals for a validated period.
 *
 * Averages are `null` when denominators are zero. No conception or transfer-success
 * rates are derived here; this reports only counts and averages with explicit denominators.
 *
 * @property eventCounts canonicalised reproduction event counts by type.
 * @property embryoCollections number of embryo-transfer collection rows in the period.
 * @property embryosCollected sum of [com.github.rodrigotimoteo.animally.domain.embryotransfer.model.EmbryoTransfer.embryoCount].
 * @property averageEmbryosPerCollection [embryosCollected] / [embryoCollections] or null.
 * @property icsiSessions number of ICSI rows in the period.
 * @property folliclesRecovered sum of follicles recovered across ICSI sessions.
 * @property averageFolliclesPerIcsi [folliclesRecovered] / [icsiSessions] or null.
 * @property ultrasoundCount number of ultrasound examinations in the period.
 */
data class ReproductionMetrics(
    val eventCounts: List<ReproductionEventCount>,
    val embryoCollections: Int,
    val embryosCollected: Int,
    val averageEmbryosPerCollection: Double?,
    val icsiSessions: Int,
    val folliclesRecovered: Int,
    val averageFolliclesPerIcsi: Double?,
    val ultrasoundCount: Int,
)

internal fun avgOrNull(
    numerator: Int,
    denominator: Int,
): Double? = if (denominator == 0) null else numerator.toDouble() / denominator
