package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.di.infra.IosAppBridge
import com.github.rodrigotimoteo.animally.di.infra.IosSettingsStores
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetDentistryRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetVaccinationRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.presentation.ios.ReminderSettingsStore
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import kotlinx.coroutines.CoroutineDispatcher
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
import org.koin.core.qualifier.named
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Tests for the Swift-facing settings-related stores ([IosSettingsStores]),
 * including the search, timeline, reminder and Coggins sections, against a
 * real in-memory database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoresTest {
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
    fun `settings store theme mode change updates state`() =
        runTest(scheduler) {
            val store = IosSettingsStores.settingsStore()

            store.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, store.state.current)
        }

    @Test
    fun `settings store restore with blank json sets status`() =
        runTest(scheduler) {
            val store = IosSettingsStores.settingsStore()

            store.restoreBackup()

            assertNotNull(store.restoreStatus)
            assertTrue(store.restoreStatus!!.contains("Paste backup JSON first"))
        }

    @Test
    fun `search store short query keeps results empty`() =
        runTest(scheduler) {
            val store = IosSettingsStores.searchStore()

            store.setQuery("a")
            advanceUntilIdle()

            assertEquals("a", store.state.current.query)
            assertEquals(emptyList(), store.state.current.results)
            assertFalse(store.state.current.isLoading)
        }

    @Test
    fun `search store finds indexed patient`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            IosAppBridge.koin.get<ISearchRepository>().indexRecord(
                recordType = ISearchRepository.TYPE_PATIENT,
                patientId = patientId,
                recordId = patientId,
                date = null,
                searchableText = "Thunder",
            )
            val store = IosSettingsStores.searchStore()

            store.setQuery("Thunder")
            advanceUntilIdle()

            assertEquals(1, store.state.current.results.size)
            assertEquals(
                "Thunder",
                store.state.current.results
                    .single()
                    .patientName,
            )
            assertEquals(
                ISearchRepository.TYPE_PATIENT,
                store.state.current.results
                    .single()
                    .recordType,
            )
        }

    @Test
    fun `search store record type toggle filters results`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            IosAppBridge.koin.get<ISearchRepository>().indexRecord(
                recordType = ISearchRepository.TYPE_PATIENT,
                patientId = patientId,
                recordId = patientId,
                date = null,
                searchableText = "Thunder",
            )
            val store = IosSettingsStores.searchStore()

            store.setQuery("Thunder")
            advanceUntilIdle()
            assertEquals(1, store.state.current.results.size)

            store.toggleRecordType(ISearchRepository.TYPE_CONSULTATION)
            advanceUntilIdle()

            assertEquals(emptyList(), store.state.current.results)
            assertEquals(setOf(ISearchRepository.TYPE_CONSULTATION), store.state.current.recordTypes)
        }

    @Test
    fun `timeline store loads patient timeline with records`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            IosAppBridge.koin.get<IConsultationRepository>().insert(
                Consultation(
                    id = 0L,
                    patientId = patientId,
                    date = LocalDate(2025, 1, 10),
                    subjective = "Not eating well",
                    objective = "Normal vitals",
                    assessment = "Colic risk",
                    plan = "Rest and monitor",
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )
            val store = IosSettingsStores.timelineStore(patientId)

            advanceUntilIdle()

            assertEquals("Thunder", store.state.current.patientName)
            assertEquals(
                false,
                store.state.current.groups
                    .isEmpty(),
            )
            assertEquals(false, store.state.current.isLoading)
        }

    @Test
    fun `timeline store loads empty global timeline`() =
        runTest(scheduler) {
            val store = IosSettingsStores.timelineStore(patientId = null)

            advanceUntilIdle()

            assertTrue(
                store.state.current.groups
                    .isEmpty(),
            )
            assertEquals(null, store.state.current.patientName)
            assertEquals(false, store.state.current.isLoading)
        }

    @Test
    fun `reminder settings store counts due reminders without scheduling`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient(name = "Thunder"))
            IosAppBridge.koin.get<IVaccinationRepository>().insert(
                Vaccination(
                    id = 0L,
                    patientId = patientId,
                    vaccineName = "Flu A",
                    dateAdministered = LocalDate(2020, 1, 1),
                    nextDueDate = LocalDate(2020, 1, 1),
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )
            val viewModel =
                ReminderSettingsViewModel(
                    getVaccinationRemindersUseCase =
                        GetVaccinationRemindersUseCase(
                            vaccinationRepository = IosAppBridge.koin.get(),
                            patientRepository = IosAppBridge.koin.get(),
                        ),
                    getDentistryRemindersUseCase =
                        GetDentistryRemindersUseCase(
                            dentistryRepository = IosAppBridge.koin.get(),
                            patientRepository = IosAppBridge.koin.get(),
                        ),
                    ioDispatcher = IosAppBridge.koin.get<CoroutineDispatcher>(named(IO_DISPATCHER)),
                    notificationPermissionController = FakeNotificationPermissionController(granted = false),
                )
            val store = ReminderSettingsStore(viewModel)

            advanceUntilIdle()
            store.checkRemindersNow()
            advanceUntilIdle()

            assertFalse(store.state.current.remindersEnabled)
            assertEquals(1, store.state.current.lastCheckedCount)
        }

    @Test
    fun `coggins store loads empty alerts on empty database`() =
        runTest(scheduler) {
            val store = IosSettingsStores.cogginsStore()

            advanceUntilIdle()

            assertEquals(emptyList(), store.state.current.alerts)
            assertEquals(false, store.state.current.isLoading)
            store.load()
            store.dismissError()
            advanceUntilIdle()
        }
}
