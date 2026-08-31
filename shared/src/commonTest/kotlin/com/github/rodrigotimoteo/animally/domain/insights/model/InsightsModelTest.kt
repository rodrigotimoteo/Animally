package com.github.rodrigotimoteo.animally.domain.insights.model

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.model.avgOrNull
import com.github.rodrigotimoteo.animally.domain.reproduction.model.ReproductionEventType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InsightsModelTest {
    private val jan1 = LocalDate(2025, 1, 1)
    private val jan10 = LocalDate(2025, 1, 10)
    private val jan31 = LocalDate(2025, 1, 31)

    @Test
    fun insightsFilter_acceptsValidRange() {
        val filter = InsightsFilter(from = jan1, to = jan10, patientId = null)
        assertEquals(jan1, filter.from)
        assertEquals(jan10, filter.to)
        assertNull(filter.patientId)
    }

    @Test
    fun insightsFilter_allowsScopedPatient() {
        val filter = InsightsFilter(from = jan1, to = jan1, patientId = 42L)
        assertEquals(42L, filter.patientId)
    }

    @Test
    fun insightsFilter_rejectsInvalidRange() {
        assertFailsWith<IllegalArgumentException> {
            InsightsFilter(from = jan10, to = jan1)
        }
    }

    @Test
    fun insightsDrillDown_rejectsInvalidRange() {
        assertFailsWith<IllegalArgumentException> {
            InsightsDrillDown(from = jan10, to = jan1, patientId = null)
        }
    }

    @Test
    fun insightsDrillDown_requiresReproductionTypeForSubtype() {
        assertFailsWith<IllegalArgumentException> {
            InsightsDrillDown(
                from = jan1,
                to = jan10,
                recordType = RecordType.Consultation,
                reproductionEventType = ReproductionEventType.Breeding,
            )
        }
    }

    @Test
    fun metricComparison_percentageUnavailableWhenPreviousZero() {
        val cmp = MetricComparison.from(current = 5, previous = 0)
        assertEquals(5, cmp.current)
        assertEquals(0, cmp.previous)
        assertEquals(5, cmp.absoluteDelta)
        assertNull(cmp.percentageDelta)
    }

    @Test
    fun metricComparison_percentageComputedWhenPreviousNonZero() {
        val cmp = MetricComparison.from(current = 15, previous = 10)
        assertEquals(5, cmp.absoluteDelta)
        val pct = assertNotNull(cmp.percentageDelta)
        assertEquals(50.0, pct, 0.001)
    }

    @Test
    fun overviewMetrics_averagesNullForEmptyPeriod() {
        val perActive = avgOrNull(0, 0)
        val perCase = avgOrNull(0, 0)
        assertNull(perActive)
        assertNull(perCase)
    }

    @Test
    fun overviewMetrics_averagesComputedWhenNonEmpty() {
        val perActive = avgOrNull(10, 2)
        val perCase = avgOrNull(10, 5)
        assertEquals(5.0, perActive!!, 0.001)
        assertEquals(2.0, perCase!!, 0.001)
    }

    @Test
    fun recordTypeCount_shareNullForEmptyTotal() {
        assertNull(avgOrNull(0, 0))
        assertEquals(0.5, avgOrNull(1, 2)!!, 0.001)
    }

    @Test
    fun reproductionMetrics_averagesNullWhenNoCollections() {
        assertNull(avgOrNull(0, 0))
        assertNull(avgOrNull(0, 0))
    }

    @Test
    fun insightsSnapshot_emptyPeriodIsExplicit() {
        val emptyRepro =
            ReproductionMetrics(
                eventCounts = emptyList(),
                embryoCollections = 0,
                embryosCollected = 0,
                averageEmbryosPerCollection = null,
                icsiSessions = 0,
                folliclesRecovered = 0,
                averageFolliclesPerIcsi = null,
                ultrasoundCount = 0,
            )
        val snapshot =
            InsightsSnapshot(
                overview =
                    OverviewMetrics(
                        patientCount = 0,
                        activityCount = 0,
                        caseDayCount = 0,
                        activeDayCount = 0,
                        averagePerActiveDay = null,
                        averagePerCaseDay = null,
                        comparison = null,
                    ),
                activitySeries = emptyList(),
                recordMix = emptyList(),
                reproduction = emptyRepro,
                currentCare = CurrentCareSnapshot(activeGestations = emptyList()),
                dataIssues = emptyList(),
            )
        assertEquals(0, snapshot.overview.activityCount)
        assertNull(snapshot.overview.averagePerActiveDay)
        assertNull(snapshot.overview.comparison)
    }

    @Test
    fun insightsRecordRef_holdsRequiredFields() {
        val ref =
            InsightsRecordRef(
                recordType = RecordType.Vaccination,
                patientId = 1L,
                recordId = 2L,
                patientName = "Storm",
                date = jan1,
            )
        assertEquals(RecordType.Vaccination, ref.recordType)
        assertEquals(jan1, ref.date)
    }

    @Test
    fun reproductionEventCount_mapsUnknownToOther() {
        val parsed = ReproductionEventType.from("PREGNANCY_CHECK")
        assertEquals(ReproductionEventType.PregnancyCheck, parsed)
        val unknown = ReproductionEventType.from("unknown_xyz")
        assertEquals(ReproductionEventType.Other, unknown)
        val count = ReproductionEventCount(type = unknown, count = 3)
        assertEquals(ReproductionEventType.Other, count.type)
    }

    @Test
    fun currentGestationItem_holdsDaysUntilDue() {
        val item =
            CurrentGestationItem(
                patientId = 1L,
                patientName = "Mare A",
                gestationId = 10L,
                gestationDay = 100,
                dueDate = jan31,
                daysUntilDue = 21,
                status = "Active",
            )
        assertEquals(21, item.daysUntilDue)
        assertEquals(100, item.gestationDay)
    }
}
