package com.github.rodrigotimoteo.animally.domain.insights.usecase

import com.github.rodrigotimoteo.animally.domain.common.RecordType
import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentGestationItem
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetInsightsDashboardUseCaseTest {
    private val today = LocalDate(2025, 1, 31)

    private fun bucket(
        date: LocalDate,
        patientId: Long,
        type: RecordType,
        count: Int = 1,
    ) = InsightsActivityBucket(date = date, patientId = patientId, recordType = type, count = count)

    private class FakeRepository(
        var earliest: LocalDate? = null,
        var bucketsByFilter: MutableMap<InsightsFilter, List<InsightsActivityBucket>> = mutableMapOf(),
        var defaultBuckets: List<InsightsActivityBucket> = emptyList(),
        var reproductionMetrics: ReproductionMetrics =
            ReproductionMetrics(
                eventCounts = emptyList(),
                embryoCollections = 0,
                embryosCollected = 0,
                averageEmbryosPerCollection = null,
                icsiSessions = 0,
                folliclesRecovered = 0,
                averageFolliclesPerIcsi = null,
                ultrasoundCount = 0,
            ),
        var currentCareByPatient: MutableMap<Long?, CurrentCareSnapshot> = mutableMapOf(),
        var defaultCurrentCare: CurrentCareSnapshot = CurrentCareSnapshot(activeGestations = emptyList()),
        var dataIssueCounts: List<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount> = emptyList(),
        var dataIssueRefsByType: MutableMap<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType, List<InsightsRecordRef>> =
            mutableMapOf(),
    ) : IInsightsRepository {
        val capturedFilters = mutableListOf<InsightsFilter>()
        val capturedCareQueries = mutableListOf<Pair<Long?, LocalDate>>()
        val capturedReadinessFilters = mutableListOf<InsightsFilter>()

        override fun getEarliestActivityDate(patientId: Long?): LocalDate? = earliest

        override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> {
            capturedFilters.add(filter)
            return bucketsByFilter[filter] ?: defaultBuckets
        }

        override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> {
            drillDown.dataIssueType?.let { type ->
                val filter = InsightsFilter(from = drillDown.from, to = drillDown.to, patientId = drillDown.patientId)
                return getDataIssueRecordRefs(filter, type)
            }
            return emptyList()
        }

        override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics = reproductionMetrics

        override fun getCurrentCareSnapshot(
            patientId: Long?,
            today: LocalDate,
        ): CurrentCareSnapshot {
            capturedCareQueries.add(patientId to today)
            return currentCareByPatient[patientId] ?: defaultCurrentCare
        }

        override fun getDataIssueCounts(filter: InsightsFilter): List<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount> {
            capturedReadinessFilters.add(filter)
            return dataIssueCounts
        }

        override fun getDataIssueRecordRefs(
            filter: InsightsFilter,
            issueType: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType,
        ): List<InsightsRecordRef> = dataIssueRefsByType[issueType] ?: emptyList()
    }

    @Test
    fun `empty current period yields zero counts null averages empty mix empty series and comparison with zero previous`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val repo = FakeRepository(defaultBuckets = emptyList())
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)

        assertEquals(0, snap.overview.patientCount)
        assertEquals(0, snap.overview.activityCount)
        assertEquals(0, snap.overview.caseDayCount)
        assertEquals(0, snap.overview.activeDayCount)
        assertNull(snap.overview.averagePerActiveDay)
        assertNull(snap.overview.averagePerCaseDay)
        assertTrue(snap.recordMix.isEmpty())
        assertTrue(snap.activitySeries.isEmpty())
        // comparison exists and has zero previous -> percentageDelta null
        assertNotNull(snap.overview.comparison)
        assertEquals(0, snap.overview.comparison!!.current)
        assertEquals(0, snap.overview.comparison!!.previous)
        assertEquals(0, snap.overview.comparison!!.absoluteDelta)
        assertNull(snap.overview.comparison!!.percentageDelta)
        // two repository calls with exact non-overlapping inclusive ranges
        assertEquals(2, repo.capturedFilters.size)
        val current = repo.capturedFilters[0]
        val comp = repo.capturedFilters[1]
        assertEquals(filter.from, current.from)
        assertEquals(filter.to, current.to)
        assertTrue(comp.to < current.from)
        assertEquals(11, current.from.daysUntil(current.to) + 1)
        assertEquals(11, comp.from.daysUntil(comp.to) + 1)
        assertEquals(current.from.minus(DatePeriod(days = 1)), comp.to)
        assertEquals(current.from.minus(DatePeriod(days = 11)), comp.from)
    }

    @Test
    fun `recorded activity counts one row per source and case-day distinct pair`() {
        val d1 = LocalDate(2025, 1, 15)
        val d2 = LocalDate(2025, 1, 16)
        // two record types same patient same date -> should be 2 activities but 1 case-day and 1 activeDay
        val buckets =
            listOf(
                bucket(d1, 1L, RecordType.Consultation),
                bucket(d1, 1L, RecordType.Dentistry),
                bucket(d2, 1L, RecordType.Vaccination),
                bucket(d2, 2L, RecordType.Vaccination),
            )
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = null)
        val repo = FakeRepository()
        // map current filter to buckets, comparison filter to empty
        val sut = GetInsightsDashboardUseCase(repo) { today }
        val compFilter = filter.comparisonRange()
        repo.bucketsByFilter[filter] = buckets
        repo.bucketsByFilter[compFilter] = emptyList()

        val snap = sut(filter)

        assertEquals(4, snap.overview.activityCount)
        assertEquals(2, snap.overview.patientCount)
        // case-days: (1,d1), (1,d2), (2,d2) => 3
        assertEquals(3, snap.overview.caseDayCount)
        // active days: d1, d2 =>2
        assertEquals(2, snap.overview.activeDayCount)
        // averages
        assertEquals(2.0, snap.overview.averagePerActiveDay!!, 0.001)
        assertEquals(4.0 / 3.0, snap.overview.averagePerCaseDay!!, 0.001)
        // current/comparison non overlapping verified
        assertEquals(2, repo.capturedFilters.size)
        assertEquals(filter, repo.capturedFilters[0])
        assertEquals(compFilter, repo.capturedFilters[1])
    }

    @Test
    fun `patients seen distinct and record share computed safely`() {
        val d = LocalDate(2025, 1, 15)
        val buckets =
            listOf(
                bucket(d, 1L, RecordType.Consultation),
                bucket(d, 1L, RecordType.Consultation, count = 1),
                bucket(d, 2L, RecordType.Vaccination),
            )
        // Actually buckets are grouped by date/patient/type, but second consult same key would be count 2
        // Simulate that as two buckets merging? Use counts.
        val merged =
            listOf(
                bucket(d, 1L, RecordType.Consultation, count = 2),
                bucket(d, 2L, RecordType.Vaccination, count = 1),
            )
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val repo = FakeRepository()
        val comp = filter.comparisonRange()
        repo.bucketsByFilter[filter] = merged
        repo.bucketsByFilter[comp] = emptyList()
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)
        assertEquals(3, snap.overview.activityCount)
        assertEquals(2, snap.overview.patientCount)
        // record mix
        assertEquals(2, snap.recordMix.size)
        val consult = snap.recordMix.first { it.type == RecordType.Consultation }
        val vac = snap.recordMix.first { it.type == RecordType.Vaccination }
        assertEquals(2, consult.count)
        assertEquals(1, vac.count)
        assertEquals(2.0 / 3.0, consult.share!!, 0.001)
        assertEquals(1.0 / 3.0, vac.share!!, 0.001)
        // total = 3, shares sum to 1.0 within rounding
        val sum = snap.recordMix.sumOf { it.share!! }
        assertEquals(1.0, sum, 0.001)
    }

    @Test
    fun `zero denominators yield null not zero or NaN`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val repo = FakeRepository(defaultBuckets = emptyList())
        val sut = GetInsightsDashboardUseCase(repo) { today }
        val snap = sut(filter)
        assertNull(snap.overview.averagePerActiveDay)
        assertNull(snap.overview.averagePerCaseDay)
        assertTrue(snap.recordMix.isEmpty())
        // ensure no share is zero when empty, and no NaN
        snap.recordMix.forEach {
            assertNotNull(it.share)
            assertTrue(it.share!!.isFinite())
        }
        // comparison percentageDelta null when previous zero
        assertNull(snap.overview.comparison!!.percentageDelta)
        assertTrue(
            snap.overview.comparison!!
                .percentageDelta
                ?.isFinite() ?: true,
        )
    }

    @Test
    fun `record share null when empty period`() {
        val d = LocalDate(2025, 1, 15)
        val buckets = listOf(bucket(d, 1L, RecordType.Consultation))
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val repo = FakeRepository()
        repo.bucketsByFilter[filter] = buckets
        repo.bucketsByFilter[filter.comparisonRange()] = emptyList()
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)
        assertEquals(1, snap.recordMix.size)
        assertEquals(1.0, snap.recordMix.single().share!!, 0.001)

        // empty case already covered -> empty list, share not zero
        val emptySnap = GetInsightsDashboardUseCase(FakeRepository()) { today }.invoke(filter)
        assertTrue(emptySnap.recordMix.isEmpty())
    }

    @Test
    fun `comparison range is N days ending from minus one day inclusive`() {
        val cases =
            listOf(
                InsightsFilter(LocalDate(2025, 1, 15), LocalDate(2025, 1, 15)) to 1,
                InsightsFilter(LocalDate(2025, 1, 2), LocalDate(2025, 1, 31)) to 30,
                InsightsFilter(LocalDate(2025, 1, 10), LocalDate(2025, 1, 19)) to 10,
                InsightsFilter(LocalDate(2025, 2, 28), LocalDate(2025, 3, 2)) to 3,
            )
        for ((filter, n) in cases) {
            val comp = filter.comparisonRange()
            // inclusive length matches N
            assertEquals(n, filter.from.daysUntil(filter.to) + 1)
            assertEquals(n, comp.from.daysUntil(comp.to) + 1)
            // ends on from-1
            assertEquals(filter.from.minus(DatePeriod(days = 1)), comp.to)
            // no overlap: comp.to < filter.from
            assertTrue(comp.to < filter.from)
            assertTrue(comp.from <= comp.to)
        }
    }

    @Test
    fun `invokes exact non-overlapping inclusive ranges for current and comparison`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 20), to = LocalDate(2025, 1, 31))
        val repo = FakeRepository()
        val expectedComp = filter.comparisonRange()
        repo.bucketsByFilter[filter] = listOf(bucket(LocalDate(2025, 1, 25), 1L, RecordType.LabResult))
        repo.bucketsByFilter[expectedComp] = listOf(bucket(LocalDate(2025, 1, 10), 1L, RecordType.LabResult))
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)

        assertEquals(2, repo.capturedFilters.size)
        assertEquals(filter.from, repo.capturedFilters[0].from)
        assertEquals(filter.to, repo.capturedFilters[0].to)
        assertEquals(expectedComp.from, repo.capturedFilters[1].from)
        assertEquals(expectedComp.to, repo.capturedFilters[1].to)
        assertEquals(filter.patientId, repo.capturedFilters[1].patientId)
        assertEquals(1, snap.overview.activityCount)
        // comparison: current 1, previous 1 => delta 0, pct 0
        assertEquals(0, snap.overview.comparison!!.absoluteDelta)
        assertEquals(0.0, snap.overview.comparison!!.percentageDelta!!, 0.001)
    }

    @Test
    fun `daily buckets through 45 days`() {
        val from = LocalDate(2025, 1, 1)
        val to = LocalDate(2025, 1, 10) // 10 days <=45 daily
        assertEquals(BucketGranularity.DAILY, InsightsBucketing.granularity(from, to))
        val b45From = LocalDate(2025, 1, 1)
        val b45To = LocalDate(2025, 2, 14) // 45 days Jan1-Feb14 inclusive =45
        assertEquals(45, b45From.daysUntil(b45To) + 1)
        assertEquals(BucketGranularity.DAILY, InsightsBucketing.granularity(b45From, b45To))
    }

    @Test
    fun `weekly buckets 46 to 180 days`() {
        val w46From = LocalDate(2025, 1, 1)
        val w46To = LocalDate(2025, 2, 15) // 46 days
        assertEquals(46, w46From.daysUntil(w46To) + 1)
        assertEquals(BucketGranularity.WEEKLY, InsightsBucketing.granularity(w46From, w46To))
        val w180From = LocalDate(2025, 1, 1)
        val w180To = w180From.plus(DatePeriod(days = 179)) // inclusive 180
        assertEquals(180, w180From.daysUntil(w180To) + 1)
        assertEquals(BucketGranularity.WEEKLY, InsightsBucketing.granularity(w180From, w180To))
    }

    @Test
    fun `monthly buckets above 180 days`() {
        val m181From = LocalDate(2025, 1, 1)
        val m181To = m181From.plus(DatePeriod(days = 180)) // inclusive 181
        assertEquals(181, m181From.daysUntil(m181To) + 1)
        assertEquals(BucketGranularity.MONTHLY, InsightsBucketing.granularity(m181From, m181To))
    }

    @Test
    fun `activity series uses daily granularity correctly`() {
        val from = LocalDate(2025, 1, 1)
        val to = LocalDate(2025, 1, 10) // daily
        val buckets =
            listOf(
                bucket(LocalDate(2025, 1, 2), 1L, RecordType.Consultation, count = 2),
                bucket(LocalDate(2025, 1, 2), 2L, RecordType.Vaccination, count = 1),
                bucket(LocalDate(2025, 1, 5), 1L, RecordType.LabResult, count = 3),
            )
        val series = InsightsBucketing.buildSeries(buckets, from, to)
        // daily: group by date
        assertEquals(2, series.size)
        assertEquals(LocalDate(2025, 1, 2), series[0].periodStart)
        assertEquals(3, series[0].count)
        assertEquals(LocalDate(2025, 1, 5), series[1].periodStart)
        assertEquals(3, series[1].count)
    }

    @Test
    fun `activity series uses weekly calendar buckets`() {
        val from = LocalDate(2025, 1, 1)
        val to = from.plus(DatePeriod(days = 59)) // 60 days -> weekly
        assertEquals(BucketGranularity.WEEKLY, InsightsBucketing.granularity(from, to))
        // Week Monday for 2025-01-15 (Wednesday) is 2025-01-13, for 2025-01-17 (Friday) same week
        val w1Mon = LocalDate(2025, 1, 13)
        val w2Mon = LocalDate(2025, 1, 20)
        val buckets =
            listOf(
                bucket(LocalDate(2025, 1, 15), 1L, RecordType.Consultation),
                bucket(LocalDate(2025, 1, 17), 1L, RecordType.Dentistry),
                bucket(LocalDate(2025, 1, 20), 1L, RecordType.Vaccination),
            )
        val series = InsightsBucketing.buildSeries(buckets, from, to)
        assertEquals(2, series.size)
        assertEquals(w1Mon, series[0].periodStart)
        assertEquals(2, series[0].count)
        assertEquals(w2Mon, series[1].periodStart)
        assertEquals(1, series[1].count)
    }

    @Test
    fun `activity series uses monthly calendar buckets`() {
        val from = LocalDate(2025, 1, 1)
        val to = from.plus(DatePeriod(days = 200)) // >180 monthly
        assertEquals(BucketGranularity.MONTHLY, InsightsBucketing.granularity(from, to))
        val buckets =
            listOf(
                bucket(LocalDate(2025, 1, 10), 1L, RecordType.Consultation),
                bucket(LocalDate(2025, 1, 20), 1L, RecordType.Dentistry),
                bucket(LocalDate(2025, 2, 5), 1L, RecordType.Vaccination),
                bucket(LocalDate(2025, 3, 1), 1L, RecordType.LabResult),
            )
        val series = InsightsBucketing.buildSeries(buckets, from, to)
        assertEquals(3, series.size)
        assertEquals(LocalDate(2025, 1, 1), series[0].periodStart)
        assertEquals(2, series[0].count)
        assertEquals(LocalDate(2025, 2, 1), series[1].periodStart)
        assertEquals(1, series[1].count)
        assertEquals(LocalDate(2025, 3, 1), series[2].periodStart)
    }

    @Test
    fun `all time earliest through today captured`() {
        val earliest = LocalDate(2025, 1, 5)
        val repo = FakeRepository(earliest = earliest)
        val expectedFilter = InsightsFilter(from = earliest, to = today, patientId = null)
        val buckets = listOf(bucket(LocalDate(2025, 1, 10), 1L, RecordType.Consultation))
        repo.bucketsByFilter[expectedFilter] = buckets
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = null)

        assertEquals(1, repo.capturedFilters.size)
        assertEquals(expectedFilter, repo.capturedFilters.single())
        assertEquals(1, snap.overview.activityCount)
        assertNull(snap.overview.comparison)
        assertEquals(expectedFilter, snap.appliedFilter)
        // activity series should be built with range earliest..today
        assertEquals(1, snap.activitySeries.size)
    }

    @Test
    fun `all time empty DB returns empty state not invented range`() {
        val repo = FakeRepository(earliest = null)
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = null)

        assertTrue(repo.capturedFilters.isEmpty())
        assertEquals(0, snap.overview.activityCount)
        assertEquals(0, snap.overview.patientCount)
        assertEquals(0, snap.overview.caseDayCount)
        assertEquals(0, snap.overview.activeDayCount)
        assertNull(snap.overview.averagePerActiveDay)
        assertNull(snap.overview.averagePerCaseDay)
        assertNull(snap.overview.comparison)
        assertTrue(snap.activitySeries.isEmpty())
        assertTrue(snap.recordMix.isEmpty())
        assertNull(snap.appliedFilter)
    }

    @Test
    fun `all time with only future activity returns empty snapshot safely`() {
        val repo = FakeRepository(earliest = today.plus(DatePeriod(days = 1)))
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = null)

        assertTrue(repo.capturedFilters.isEmpty())
        assertEquals(0, snap.overview.activityCount)
        assertNull(snap.appliedFilter)
    }

    @Test
    fun `all time scoped by patientId uses same earliest scope`() {
        val earliest = LocalDate(2025, 1, 10)
        val repo = FakeRepository(earliest = earliest)
        val expected = InsightsFilter(from = earliest, to = today, patientId = 42L)
        repo.bucketsByFilter[expected] = emptyList()
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = 42L)
        assertEquals(expected, repo.capturedFilters.single())
        assertEquals(0, snap.overview.activityCount)
    }

    @Test
    fun `global metrics respect patientId scope in comparison filter`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = 7L)
        val repo = FakeRepository()
        repo.bucketsByFilter[filter] = listOf(bucket(LocalDate(2025, 1, 15), 7L, RecordType.Consultation))
        val comp = filter.comparisonRange()
        repo.bucketsByFilter[comp] = emptyList()
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)
        assertEquals(7L, repo.capturedFilters[0].patientId)
        assertEquals(7L, repo.capturedFilters[1].patientId)
        assertEquals(1, snap.overview.patientCount)
    }

    @Test
    fun `percentageDelta null when previous zero else computed`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20))
        val repo = FakeRepository()
        // current 5, previous 0 => pct null
        repo.bucketsByFilter[filter] = listOf(bucket(LocalDate(2025, 1, 15), 1L, RecordType.Consultation, count = 5))
        repo.bucketsByFilter[filter.comparisonRange()] = emptyList()
        val sut = GetInsightsDashboardUseCase(repo) { today }
        val snap1 = sut(filter)
        assertNull(snap1.overview.comparison!!.percentageDelta)

        // current 15, previous 10 => +50%
        val repo2 = FakeRepository()
        repo2.bucketsByFilter[filter] = listOf(bucket(LocalDate(2025, 1, 15), 1L, RecordType.Consultation, count = 15))
        repo2.bucketsByFilter[filter.comparisonRange()] = listOf(bucket(LocalDate(2025, 1, 5), 1L, RecordType.Consultation, count = 10))
        val sut2 = GetInsightsDashboardUseCase(repo2) { today }
        val snap2 = sut2(filter)
        assertEquals(50.0, snap2.overview.comparison!!.percentageDelta!!, 0.001)
        assertEquals(5, snap2.overview.comparison!!.absoluteDelta)
        // current 5 previous 10 => -50%
        val repo3 = FakeRepository()
        repo3.bucketsByFilter[filter] = listOf(bucket(LocalDate(2025, 1, 15), 1L, RecordType.Consultation, count = 5))
        repo3.bucketsByFilter[filter.comparisonRange()] = listOf(bucket(LocalDate(2025, 1, 5), 1L, RecordType.Consultation, count = 10))
        val snap3 = GetInsightsDashboardUseCase(repo3) { today }.invoke(filter)
        assertEquals(-50.0, snap3.overview.comparison!!.percentageDelta!!, 0.001)
    }

    @Test
    fun `invoke with null filter delegates to all time via canonical`() {
        val earliest = LocalDate(2025, 1, 1)
        val repo = FakeRepository(earliest = earliest)
        val expected = InsightsFilter(from = earliest, to = today, patientId = null)
        repo.bucketsByFilter[expected] = listOf(bucket(LocalDate(2025, 1, 10), 1L, RecordType.Consultation))
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = null)
        assertEquals(expected, repo.capturedFilters.single())
        assertEquals(1, snap.overview.activityCount)
        assertNull(snap.overview.comparison)
    }

    // ---- Task 11: current gestation snapshot ----

    @Test
    fun `gestation snapshot is independent of period range and uses injected today`() {
        val filter = InsightsFilter(from = LocalDate(2025, 1, 10), to = LocalDate(2025, 1, 20), patientId = 7L)
        val care =
            CurrentCareSnapshot(
                activeGestations =
                    listOf(
                        CurrentGestationItem(
                            patientId = 1L,
                            patientName = "Star",
                            gestationId = 99L,
                            gestationDay = 300,
                            dueDate = LocalDate(2025, 2, 20),
                            daysUntilDue = 20,
                            status = "Active",
                        ),
                    ),
            )
        val repo = FakeRepository()
        repo.bucketsByFilter[filter] = emptyList()
        repo.bucketsByFilter[filter.comparisonRange()] = emptyList()
        repo.currentCareByPatient[7L] = care
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut(filter)

        assertEquals(1, snap.currentCare.activeGestations.size)
        assertEquals(7L, repo.capturedCareQueries.single().first)
        assertEquals(today, repo.capturedCareQueries.single().second)
        // second call for comparison must not re-query care; only one care query per snapshot
        assertEquals(1, repo.capturedCareQueries.size)
    }

    @Test
    fun `overdue gestations have negative daysUntilDue and excluded from dueSoon`() {
        val today = LocalDate(2025, 1, 31)
        val care =
            CurrentCareSnapshot(
                activeGestations =
                    listOf(
                        CurrentGestationItem(1L, "A", 1L, 350, LocalDate(2025, 1, 20), -11, "Active"),
                        CurrentGestationItem(2L, "B", 2L, 340, LocalDate(2025, 1, 31), 0, "Active"),
                        CurrentGestationItem(3L, "C", 3L, 330, LocalDate(2025, 2, 10), 10, "Active"),
                        CurrentGestationItem(4L, "D", 4L, 310, LocalDate(2025, 3, 1), 29, "Active"),
                        CurrentGestationItem(5L, "E", 5L, 300, LocalDate(2025, 3, 15), 43, "Active"),
                    ),
            )
        val overdue = care.activeGestations.first { it.daysUntilDue < 0 }
        assertEquals(-11, overdue.daysUntilDue)
        // simulate repo building dueSoon from same list: dueSoon filters 0..N inclusive
        val due30 = care.activeGestations.filter { it.daysUntilDue in 0..30 }
        val due60 = care.activeGestations.filter { it.daysUntilDue in 0..60 }
        val due90 = care.activeGestations.filter { it.daysUntilDue in 0..90 }
        assertEquals(3, due30.size) // 0,10,29
        assertEquals(4, due60.size) // +43
        assertEquals(4, due90.size)
        assertTrue(due30.none { it.daysUntilDue < 0 })
    }

    @Test
    fun `all time snapshot includes currentCare even when no activity`() {
        val today = LocalDate(2025, 1, 31)
        val care =
            CurrentCareSnapshot(
                activeGestations =
                    listOf(
                        CurrentGestationItem(1L, "Star", 1L, 100, LocalDate(2025, 5, 1), 90, "Active"),
                    ),
            )
        val repo = FakeRepository(earliest = null, defaultCurrentCare = care)
        val sut = GetInsightsDashboardUseCase(repo) { today }

        val snap = sut.getAllTimeSnapshot(patientId = null)

        assertTrue(snap.overview.activityCount == 0)
        assertEquals(1, snap.currentCare.activeGestations.size)
        assertEquals(1, repo.capturedCareQueries.size)
        assertEquals(null, repo.capturedCareQueries.single().first)
    }
}
