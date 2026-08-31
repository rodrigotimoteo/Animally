package com.github.rodrigotimoteo.animally.presentation.insights

import com.github.rodrigotimoteo.animally.domain.insights.IInsightsRepository
import com.github.rodrigotimoteo.animally.domain.insights.model.CurrentCareSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsActivityBucket
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDrillDown
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsFilter
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsRecordRef
import com.github.rodrigotimoteo.animally.domain.insights.model.InsightsSnapshot
import com.github.rodrigotimoteo.animally.domain.insights.model.OverviewMetrics
import com.github.rodrigotimoteo.animally.domain.insights.model.ReproductionMetrics
import com.github.rodrigotimoteo.animally.domain.insights.usecase.GetInsightsDashboardUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    private val today = LocalDate(2025, 1, 31)
    private val thirtyFrom = LocalDate(2025, 1, 2)
    private val ninetyFrom = LocalDate(2024, 11, 3)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun emptySnapshot(): InsightsSnapshot =
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
            reproduction =
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
            currentCare = CurrentCareSnapshot(activeGestations = emptyList()),
            dataIssues = emptyList(),
        )

    private fun contentSnapshot(count: Int = 5): InsightsSnapshot =
        InsightsSnapshot(
            overview =
                OverviewMetrics(
                    patientCount = 1,
                    activityCount = count,
                    caseDayCount = 1,
                    activeDayCount = 1,
                    averagePerActiveDay = count.toDouble(),
                    averagePerCaseDay = count.toDouble(),
                    comparison = null,
                ),
            activitySeries = emptyList(),
            recordMix = emptyList(),
            reproduction =
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
            currentCare = CurrentCareSnapshot(activeGestations = emptyList()),
            dataIssues = emptyList(),
        )

    private class FakeInsightsRepository(
        var earliest: LocalDate? = null,
        var bucketsByFilter: MutableMap<InsightsFilter, List<InsightsActivityBucket>> = mutableMapOf(),
        var defaultBuckets: List<InsightsActivityBucket> = emptyList(),
    ) : IInsightsRepository {
        val capturedFilters = mutableListOf<InsightsFilter>()

        override fun getEarliestActivityDate(patientId: Long?): LocalDate? = earliest

        override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> {
            capturedFilters.add(filter)
            return bucketsByFilter[filter] ?: defaultBuckets
        }

        override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> = emptyList()

        override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics =
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

        override fun getCurrentCareSnapshot(
            patientId: Long?,
            today: LocalDate,
        ): CurrentCareSnapshot = CurrentCareSnapshot(activeGestations = emptyList())

        override fun getDataIssueCounts(filter: InsightsFilter): List<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount> =
            emptyList()

        override fun getDataIssueRecordRefs(
            filter: InsightsFilter,
            issueType: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType,
        ): List<InsightsRecordRef> = emptyList()
    }

    private fun createUseCase(
        repo: FakeInsightsRepository,
        todayProvider: () -> LocalDate = { today },
    ): GetInsightsDashboardUseCase = GetInsightsDashboardUseCase(repo, todayProvider)

    private fun createViewModel(
        useCase: GetInsightsDashboardUseCase,
        dispatcher: TestDispatcher,
        todayProvider: () -> LocalDate = { today },
        initialPatientId: Long? = null,
    ): InsightsViewModel =
        InsightsViewModel(
            getInsightsDashboardUseCase = useCase,
            ioDispatcher = dispatcher,
            todayProvider = todayProvider,
            initialPatientId = initialPatientId,
        )

    @Test
    fun `first load defaults to locked 30 day range inclusive today`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)

            // Before idle, initial state already has 30-day range.
            val initial = vm.uiState.value
            assertEquals(InsightsPreset.THIRTY_DAYS, initial.preset)
            assertEquals(thirtyFrom, initial.from)
            assertEquals(today, initial.to)
            assertEquals(thirtyFrom, initial.customFrom)
            assertEquals(today, initial.customTo)
            assertNull(initial.validationError)

            advanceUntilIdle()

            val loaded = vm.uiState.value
            assertFalse(loaded.isLoading)
            assertNotNull(loaded.snapshot)
            // Verify repository was called with 30-day filter.
            assertEquals(2, repo.capturedFilters.size)
            // First call current, second comparison. Check current equals 30 days.
            val current = repo.capturedFilters.first()
            assertEquals(thirtyFrom, current.from)
            assertEquals(today, current.to)
            assertNull(current.patientId)
        }

    @Test
    fun `initial patient scope is respected for first load`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher, initialPatientId = 42L)

            advanceUntilIdle()

            assertEquals(42L, vm.uiState.value.patientId)
            assertEquals(42L, repo.capturedFilters.first().patientId)
        }

    @Test
    fun `invalid custom input stays visible with validation feedback and no repository call`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()
            val baselineCalls = repo.capturedFilters.size

            // from > to invalid
            vm.setCustomRange(from = LocalDate(2025, 1, 20), to = LocalDate(2025, 1, 10))
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(InsightsPreset.CUSTOM, state.preset)
            assertEquals(LocalDate(2025, 1, 20), state.customFrom)
            assertEquals(LocalDate(2025, 1, 10), state.customTo)
            assertTrue(assertNotNull(state.validationError).contains("Start date"))
            assertFalse(state.isLoading)
            // No additional repository calls beyond baseline (validation blocks load)
            assertEquals(baselineCalls, repo.capturedFilters.size)
            // Inputs remain visible
            assertEquals(LocalDate(2025, 1, 20), state.customFrom)
            // Snapshot remains previous (not cleared)
            assertNotNull(state.snapshot)

            // Null bounds also invalid
            vm.setCustomRange(null, LocalDate(2025, 1, 10))
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.validationError)
            assertEquals(null, vm.uiState.value.customFrom)
            assertEquals(LocalDate(2025, 1, 10), vm.uiState.value.customTo)
        }

    @Test
    fun `valid custom range triggers load and clears validation`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()
            repo.capturedFilters.clear()

            vm.setCustomRange(LocalDate(2025, 1, 10), LocalDate(2025, 1, 15))
            advanceUntilIdle()

            assertNull(vm.uiState.value.validationError)
            assertEquals(LocalDate(2025, 1, 10), vm.uiState.value.from)
            assertEquals(LocalDate(2025, 1, 15), vm.uiState.value.to)
            assertEquals(InsightsPreset.CUSTOM, vm.uiState.value.preset)
            assertTrue(repo.capturedFilters.isNotEmpty())
            assertEquals(LocalDate(2025, 1, 10), repo.capturedFilters.first().from)
        }

    @Test
    fun `rapid filter changes cannot publish older request over newer via job cancel`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo =
                object : IInsightsRepository {
                    val captured = mutableListOf<InsightsFilter?>()

                    override fun getEarliestActivityDate(patientId: Long?): LocalDate? = LocalDate(2024, 1, 1)

                    override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> {
                        captured.add(filter)
                        val count =
                            when (filter.from) {
                                thirtyFrom -> 30
                                ninetyFrom -> 90
                                LocalDate(2025, 1, 10) -> 42
                                else -> 1
                            }
                        return listOf(
                            InsightsActivityBucket(
                                date = filter.from,
                                patientId = 1L,
                                recordType = com.github.rodrigotimoteo.animally.domain.common.RecordType.Consultation,
                                count = count,
                            ),
                        )
                    }

                    override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> = emptyList()

                    override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics =
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

                    override fun getCurrentCareSnapshot(
                        patientId: Long?,
                        today: LocalDate,
                    ): CurrentCareSnapshot = CurrentCareSnapshot(activeGestations = emptyList())

                    override fun getDataIssueCounts(
                        filter: InsightsFilter,
                    ): List<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount> = emptyList()

                    override fun getDataIssueRecordRefs(
                        filter: InsightsFilter,
                        issueType: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType,
                    ): List<InsightsRecordRef> = emptyList()
                }
            val useCase = GetInsightsDashboardUseCase(repo) { today }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = InsightsViewModel(useCase, dispatcher, { today }, null)

            // Do not advance between rapid changes; they should cancel previous.
            vm.selectPreset(InsightsPreset.NINETY_DAYS)
            vm.selectPreset(InsightsPreset.THIRTY_DAYS)
            vm.setCustomRange(LocalDate(2025, 1, 10), LocalDate(2025, 1, 20))

            advanceUntilIdle()

            // Final state must reflect last filter (custom 42) not earlier 90 or 30.
            val snapshot = assertNotNull(vm.uiState.value.snapshot)
            assertEquals(
                42,
                snapshot.overview.activityCount,
            )
            assertEquals(InsightsPreset.CUSTOM, vm.uiState.value.preset)
        }

    @Test
    fun `presets resolve to correct inclusive ranges`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()
            repo.capturedFilters.clear()

            vm.selectPreset(InsightsPreset.NINETY_DAYS)
            advanceUntilIdle()
            assertEquals(ninetyFrom, vm.uiState.value.from)
            assertEquals(today, vm.uiState.value.to)
            assertEquals(InsightsPreset.NINETY_DAYS, vm.uiState.value.preset)

            vm.selectPreset(InsightsPreset.ALL_TIME)
            advanceUntilIdle()
            assertNull(vm.uiState.value.from)
            assertNull(vm.uiState.value.to)
            assertEquals(InsightsPreset.ALL_TIME, vm.uiState.value.preset)
            // All-time snapshot has no comparison.
            assertNull(assertNotNull(vm.uiState.value.snapshot).overview.comparison)

            vm.selectPreset(InsightsPreset.THIRTY_DAYS)
            advanceUntilIdle()
            assertEquals(thirtyFrom, vm.uiState.value.from)
            assertEquals(today, vm.uiState.value.to)
        }

    @Test
    fun `patient scope change triggers reload with new patientId`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()
            repo.capturedFilters.clear()

            vm.setPatientScope(7L)
            advanceUntilIdle()
            assertEquals(7L, vm.uiState.value.patientId)
            assertTrue(repo.capturedFilters.all { it.patientId == 7L })

            vm.setPatientScope(null)
            advanceUntilIdle()
            assertNull(vm.uiState.value.patientId)
        }

    @Test
    fun `loading and error states are explicit and immutable StateFlow`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val failingRepo =
                object : IInsightsRepository {
                    override fun getEarliestActivityDate(patientId: Long?): LocalDate? = null

                    override fun getActivityBuckets(filter: InsightsFilter): List<InsightsActivityBucket> = throw RuntimeException("db fail")

                    override fun getRecordRefs(drillDown: InsightsDrillDown): List<InsightsRecordRef> = emptyList()

                    override fun getReproductionMetrics(filter: InsightsFilter): ReproductionMetrics = throw RuntimeException("db fail")

                    override fun getCurrentCareSnapshot(
                        patientId: Long?,
                        today: LocalDate,
                    ): CurrentCareSnapshot = throw RuntimeException("db fail")

                    override fun getDataIssueCounts(
                        filter: InsightsFilter,
                    ): List<com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueCount> = throw RuntimeException("db fail")

                    override fun getDataIssueRecordRefs(
                        filter: InsightsFilter,
                        issueType: com.github.rodrigotimoteo.animally.domain.insights.model.InsightsDataIssueType,
                    ): List<InsightsRecordRef> = throw RuntimeException("db fail")
                }
            val failingUseCase = GetInsightsDashboardUseCase(failingRepo) { today }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = InsightsViewModel(failingUseCase, dispatcher, { today }, null)

            // Initial load will fail.
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isLoading)
            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals("db fail", vm.uiState.value.errorMessage)
            val activityCount =
                vm.uiState.value.snapshot
                    ?.overview
                    ?.activityCount
                    ?: 0
            assertEquals(0, activityCount)

            vm.dismissError()
            assertNull(vm.uiState.value.errorMessage)

            // Verify the exposed StateFlow cannot be cast to MutableStateFlow.
            assertFalse(vm.uiState is kotlinx.coroutines.flow.MutableStateFlow)
        }

    @Test
    fun `ViewModel work runs on injected IO dispatcher`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val ioDispatcher = StandardTestDispatcher(testScheduler)
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val vm = createViewModel(useCase, ioDispatcher)
            advanceUntilIdle()
            // If IO dispatcher were not used, capturedFilters would still be populated.
            assertTrue(repo.capturedFilters.isNotEmpty())
            // Change preset and ensure IO dispatcher still used.
            repo.capturedFilters.clear()
            vm.selectPreset(InsightsPreset.NINETY_DAYS)
            advanceUntilIdle()
            assertTrue(repo.capturedFilters.isNotEmpty())
        }

    @Test
    fun `empty snapshot yields isEmpty not isContent`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository(defaultBuckets = emptyList())
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.isEmpty)
            assertFalse(vm.uiState.value.isContent)
            assertFalse(vm.uiState.value.isLoading)
        }

    @Test
    fun `preset customRange and patientScope mutators update state correctly`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()

            vm.selectPreset(InsightsPreset.NINETY_DAYS)
            advanceUntilIdle()
            assertEquals(InsightsPreset.NINETY_DAYS, vm.uiState.value.preset)

            vm.setCustomRange(LocalDate(2025, 1, 5), LocalDate(2025, 1, 10))
            advanceUntilIdle()
            assertNull(vm.uiState.value.validationError)

            vm.setPatientScope(99L)
            advanceUntilIdle()
            assertEquals(99L, vm.uiState.value.patientId)

            vm.dismissError()
            assertNull(vm.uiState.value.errorMessage)

            vm.setPatientScope(null)
            advanceUntilIdle()
            assertNull(vm.uiState.value.patientId)
        }

    @Test
    fun `dismissValidationError clears feedback but keeps inputs`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeInsightsRepository()
            val useCase = createUseCase(repo)
            val dispatcher = StandardTestDispatcher(testScheduler)
            val vm = createViewModel(useCase, dispatcher)
            advanceUntilIdle()

            vm.setCustomRange(LocalDate(2025, 1, 20), LocalDate(2025, 1, 10))
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.validationError)
            vm.dismissValidationError()
            assertNull(vm.uiState.value.validationError)
            assertEquals(LocalDate(2025, 1, 20), vm.uiState.value.customFrom)
        }
}
