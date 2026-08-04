package com.github.rodrigotimoteo.animally

import com.github.rodrigotimoteo.animally.di.infra.IosAppBridge
import com.github.rodrigotimoteo.animally.di.infra.IosRecordStores
import com.github.rodrigotimoteo.animally.di.infra.IosReproAndDiagnosticsStores
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.model.Consultation
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
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
import kotlin.time.Instant

/**
 * Tests for the Swift-facing record-list stores, both the medical group
 * ([IosRecordStores]) and the reproduction/diagnostics group
 * ([IosReproAndDiagnosticsStores]), against a real in-memory database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordListStoresTest {
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
    fun `all record list stores resolve with patient id and expose empty state`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient())
            val consultationStore = IosRecordStores.consultationListStore(patientId)
            val vaccinationStore = IosRecordStores.vaccinationListStore(patientId)
            val dewormingStore = IosRecordStores.dewormingListStore(patientId)
            val dentistryStore = IosRecordStores.dentistryListStore(patientId)
            val farrierVisitStore = IosRecordStores.farrierVisitListStore(patientId)
            val lamenessStore = IosRecordStores.lamenessListStore(patientId)
            val surgeryStore = IosRecordStores.surgeryListStore(patientId)
            val medicationStore = IosRecordStores.medicationListStore(patientId)
            val substanceStore = IosRecordStores.substanceListStore(patientId)
            val weightStore = IosRecordStores.weightListStore(patientId)
            val reproductionStore = IosReproAndDiagnosticsStores.reproductionListStore(patientId)
            val ultrasoundStore = IosReproAndDiagnosticsStores.ultrasoundListStore(patientId)
            val gestationStore = IosReproAndDiagnosticsStores.gestationListStore(patientId)
            val reproMedicationStore = IosReproAndDiagnosticsStores.reproMedicationListStore(patientId)
            val labResultStore = IosReproAndDiagnosticsStores.labResultListStore(patientId)
            val imagingStore = IosReproAndDiagnosticsStores.imagingListStore(patientId)

            val isLoadingChecks =
                listOf(
                    { consultationStore.state.current.isLoading },
                    { vaccinationStore.state.current.isLoading },
                    { dewormingStore.state.current.isLoading },
                    { dentistryStore.state.current.isLoading },
                    { farrierVisitStore.state.current.isLoading },
                    { lamenessStore.state.current.isLoading },
                    { surgeryStore.state.current.isLoading },
                    { medicationStore.state.current.isLoading },
                    { substanceStore.state.current.isLoading },
                    { weightStore.state.current.isLoading },
                    { reproductionStore.state.current.isLoading },
                    { ultrasoundStore.state.current.isLoading },
                    { gestationStore.state.current.isLoading },
                    { reproMedicationStore.state.current.isLoading },
                    { labResultStore.state.current.isLoading },
                    { imagingStore.state.current.isLoading },
                )
            isLoadingChecks.forEach { isLoading -> assertFalse(isLoading()) }

            consultationStore.load()
            vaccinationStore.load()
            dewormingStore.load()
            dentistryStore.load()
            farrierVisitStore.load()
            lamenessStore.load()
            surgeryStore.load()
            medicationStore.load()
            substanceStore.load()
            weightStore.load()
            reproductionStore.load()
            ultrasoundStore.load()
            gestationStore.load()
            reproMedicationStore.load()
            labResultStore.load()
            imagingStore.load()
            consultationStore.dismissError()
            vaccinationStore.dismissError()
            dewormingStore.dismissError()
            dentistryStore.dismissError()
            farrierVisitStore.dismissError()
            lamenessStore.dismissError()
            surgeryStore.dismissError()
            medicationStore.dismissError()
            substanceStore.dismissError()
            weightStore.dismissError()
            reproductionStore.dismissError()
            ultrasoundStore.dismissError()
            gestationStore.dismissError()
            reproMedicationStore.dismissError()
            labResultStore.dismissError()
            imagingStore.dismissError()
            advanceUntilIdle()
        }

    @Test
    fun `consultation list store loads persisted records`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient())
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
            val store = IosRecordStores.consultationListStore(patientId)

            advanceUntilIdle()

            assertEquals(1, store.state.current.consultations.size)
            assertEquals(
                "Colic risk",
                store.state.current.consultations
                    .single()
                    .assessment,
            )
            assertEquals(false, store.state.current.isLoading)
        }

    @Test
    fun `weight list store loads persisted records`() =
        runTest(scheduler) {
            val patientId = IosAppBridge.koin.get<IPatientRepository>().insertPatient(testPatient())
            IosAppBridge.koin.get<IWeightRepository>().insert(
                Weight(
                    id = 0L,
                    patientId = patientId,
                    weightKg = 540.0,
                    date = LocalDate(2025, 1, 10),
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )
            val store = IosRecordStores.weightListStore(patientId)

            advanceUntilIdle()

            assertEquals(1, store.state.current.records.size)
            assertEquals(
                540.0,
                store.state.current.records
                    .single()
                    .weightKg,
            )
        }
}
