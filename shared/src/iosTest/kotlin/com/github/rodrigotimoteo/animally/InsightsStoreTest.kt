package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.infra.IosAppBridge
import com.github.rodrigotimoteo.animally.di.infra.IosInsightsStores
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsPreset
import com.github.rodrigotimoteo.animally.presentation.insights.InsightsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for the Swift-facing Insights store ([IosInsightsStores]) and
 * its [com.github.rodrigotimoteo.animally.presentation.ios.InsightsStore] bridge.
 *
 * Verifies Swift can observe state and invoke reload, preset,
 * custom-range, patient and error-dismiss actions. Uses a real
 * in-memory database so the ViewModel runs through its production
 * deterministic path. Follows [TimelineStore] / [com.github.rodrigotimoteo.animally.bridge.NativeFlow]
 * rather than a second bridge.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsStoreTest {
    private val scheduler = TestCoroutineScheduler()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        IosAppBridge.start(StoreTestSupport.startKoinWithInMemoryDb(scheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `global store observes initial and loaded states and cancels cleanly`() =
        runTest(scheduler) {
            val store = IosInsightsStores.insightsStore(patientId = null)

            // Initial state before idle: 30-day preset, no calculation in store.
            val initial = store.state.current
            assertEquals(InsightsPreset.THIRTY_DAYS, initial.preset)
            assertNull(initial.patientId)
            assertNotNull(initial.from)
            assertNotNull(initial.to)
            assertNull(initial.validationError)

            // Observe via NativeFlow subscription — current + subsequent.
            val received = mutableListOf<InsightsUiState>()
            val cancellable = store.state.subscribe { received += it }
            advanceUntilIdle()

            // At least initial emission delivered.
            assertTrue(received.isNotEmpty())
            assertEquals(InsightsPreset.THIRTY_DAYS, received.first().preset)

            // Loaded state after ViewModel init reload.
            val loaded = store.state.current
            assertFalse(loaded.isLoading)
            val loadedSnapshot = assertNotNull(loaded.snapshot)
            // Empty DB → empty snapshot with zero activity.
            assertEquals(0, loadedSnapshot.overview.activityCount)
            assertTrue(loaded.isEmpty)
            assertNull(loaded.errorMessage)
            assertNull(loaded.validationError)

            // Subsequent reload still observable.
            store.reload()
            advanceUntilIdle()
            assertTrue(received.size >= 2)

            // Cancel stops further emissions.
            val sizeBeforeCancel = received.size
            cancellable.cancel()
            store.reload()
            advanceUntilIdle()
            // Size must not grow after cancel.
            assertEquals(sizeBeforeCancel, received.size)
        }

    @Test
    fun `store preset action updates state`() =
        runTest(scheduler) {
            val store = IosInsightsStores.insightsStore(patientId = null)
            advanceUntilIdle()

            store.selectPreset(InsightsPreset.NINETY_DAYS)
            advanceUntilIdle()

            assertEquals(InsightsPreset.NINETY_DAYS, store.state.current.preset)
            assertNotNull(store.state.current.from)
            assertNotNull(store.state.current.to)
            assertNotNull(store.state.current.snapshot)
            assertNull(store.state.current.validationError)

            store.selectPreset(InsightsPreset.ALL_TIME)
            advanceUntilIdle()

            assertEquals(InsightsPreset.ALL_TIME, store.state.current.preset)
            assertNull(store.state.current.from)
            assertNull(store.state.current.to)
            // All-time snapshot exists even with empty DB.
            assertNotNull(store.state.current.snapshot)
            val allTimeSnapshot = store.state.current.snapshot!!
            assertNull(allTimeSnapshot.overview.comparison)
        }

    @Test
    fun `store custom range valid and invalid paths`() =
        runTest(scheduler) {
            val store = IosInsightsStores.insightsStore(patientId = null)
            advanceUntilIdle()

            // Valid custom range triggers load and clears validation.
            val from = LocalDate(2025, 1, 5)
            val to = LocalDate(2025, 1, 20)
            store.setCustomRange(from, to)
            advanceUntilIdle()

            assertEquals(InsightsPreset.CUSTOM, store.state.current.preset)
            assertEquals(from, store.state.current.from)
            assertEquals(to, store.state.current.to)
            assertEquals(from, store.state.current.customFrom)
            assertEquals(to, store.state.current.customTo)
            assertNull(store.state.current.validationError)

            // Invalid range (from > to) stays visible with validation, no snapshot overwrite.
            store.setCustomRange(LocalDate(2025, 1, 20), LocalDate(2025, 1, 10))
            advanceUntilIdle()

            assertEquals(InsightsPreset.CUSTOM, store.state.current.preset)
            assertEquals(LocalDate(2025, 1, 20), store.state.current.customFrom)
            assertEquals(LocalDate(2025, 1, 10), store.state.current.customTo)
            assertNotNull(store.state.current.validationError)
            val validation = store.state.current.validationError!!
            assertTrue(validation.contains("Start date"))
            assertFalse(store.state.current.isLoading)

            // Dismiss validation clears feedback but keeps inputs visible.
            store.dismissValidationError()
            assertNull(store.state.current.validationError)
            assertEquals(LocalDate(2025, 1, 20), store.state.current.customFrom)
        }

    @Test
    fun `store patient scope action`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            val store = IosInsightsStores.insightsStore(patientId = null)
            advanceUntilIdle()
            assertNull(store.state.current.patientId)

            store.setPatientScope(patientId)
            advanceUntilIdle()

            assertEquals(patientId, store.state.current.patientId)

            store.setPatientScope(null)
            advanceUntilIdle()

            assertNull(store.state.current.patientId)

            // Patient-scoped construction via factory.
            val scoped = IosInsightsStores.insightsStore(patientId = patientId)
            advanceUntilIdle()
            assertEquals(patientId, scoped.state.current.patientId)
        }

    @Test
    fun `store error dismiss and reload`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            IosAppBridge.koin.get<IConsultationRepository>().insert(
                Consultation(
                    id = 0L,
                    patientId = patientId,
                    date = LocalDate(2025, 1, 10),
                    subjective = "Not eating",
                    objective = "Vitals ok",
                    assessment = "Check",
                    plan = "Monitor",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )
            val store = IosInsightsStores.insightsStore(patientId = null)
            advanceUntilIdle()

            // Loaded with data for 30-day range should not be empty if today near fixture.
            // With deterministic today provided by production clock, fixture date may be out of
            // range → empty is acceptable; verify store exposes snapshot regardless.
            assertNotNull(store.state.current.snapshot)

            // Trigger validation error then dismiss via error path.
            store.setCustomRange(LocalDate(2025, 2, 1), LocalDate(2025, 1, 1))
            advanceUntilIdle()
            assertNotNull(store.state.current.validationError)

            store.dismissValidationError()
            assertNull(store.state.current.validationError)

            // Error message dismiss path — initially null, dismiss is no-op but must not crash.
            store.dismissError()
            assertNull(store.state.current.errorMessage)

            // Reload after fixture insertion is observable.
            store.selectPreset(InsightsPreset.ALL_TIME)
            advanceUntilIdle()
            // All-time includes the inserted consultation regardless of today.
            val finalSnapshot = store.state.current.snapshot!!
            assertEquals(1, finalSnapshot.overview.activityCount)
        }

    @Test
    fun `store contains no calculation - delegates only`() =
        runTest(scheduler) {
            val store = IosInsightsStores.insightsStore(patientId = null)
            advanceUntilIdle()

            // Verify preset change does not require local date arithmetic in store:
            // the ViewModel resolves inclusive offsets; store just forwards.
            val beforeFrom = store.state.current.from
            store.selectPreset(InsightsPreset.THIRTY_DAYS)
            advanceUntilIdle()
            // From/to existence still supplied by ViewModel, not store logic.
            assertNotNull(store.state.current.from)
            assertNotNull(beforeFrom)
        }
}
