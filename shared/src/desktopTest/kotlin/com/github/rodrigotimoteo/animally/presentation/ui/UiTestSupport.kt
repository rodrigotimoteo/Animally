package com.github.rodrigotimoteo.animally.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.data.anamnese.AnamneseRepositoryImpl
import com.github.rodrigotimoteo.animally.data.consultation.ConsultationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.dentistry.DentistryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.deworming.DewormingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.farrier.FarrierVisitRepositoryImpl
import com.github.rodrigotimoteo.animally.data.gestation.GestationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.imaging.ImagingRepositoryImpl
import com.github.rodrigotimoteo.animally.data.labresult.LabResultRepositoryImpl
import com.github.rodrigotimoteo.animally.data.lameness.LamenessRepositoryImpl
import com.github.rodrigotimoteo.animally.data.medication.MedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.owner.OwnerRepositoryImpl
import com.github.rodrigotimoteo.animally.data.patient.PatientRepositoryImpl
import com.github.rodrigotimoteo.animally.data.reproduction.ReproductionRepositoryImpl
import com.github.rodrigotimoteo.animally.data.repromedication.ReproMedicationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.search.SearchRepositoryImpl
import com.github.rodrigotimoteo.animally.data.substance.ControlledSubstanceRepositoryImpl
import com.github.rodrigotimoteo.animally.data.surgery.SurgeryRepositoryImpl
import com.github.rodrigotimoteo.animally.data.ultrasound.UltrasoundRepositoryImpl
import com.github.rodrigotimoteo.animally.data.vaccination.VaccinationRepositoryImpl
import com.github.rodrigotimoteo.animally.data.weight.WeightRepositoryImpl
import com.github.rodrigotimoteo.animally.di.database.databaseTestModules
import com.github.rodrigotimoteo.animally.di.dispatchers.IO_DISPATCHER
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.backup.ExportBackupUseCase
import com.github.rodrigotimoteo.animally.domain.backup.RestoreBackupUseCase
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.consultation.usecase.GetConsultationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.usecase.GetDentistryListByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.deworming.usecase.GetDewormingsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.export.CsvExporter
import com.github.rodrigotimoteo.animally.domain.export.ExportBasicRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportClinicalRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportCsvUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportReproductiveRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.ExportPatientReportUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.farrier.usecase.GetFarrierVisitsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.gestation.usecase.GetGestationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.imaging.usecase.GetImagingListByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.labresult.usecase.GetLabResultsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.lameness.usecase.GetLamenessListByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.medication.usecase.GetMedicationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.notification.NotificationPermissionController
import com.github.rodrigotimoteo.animally.domain.notification.NotificationPermissionControllerImpl
import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.owner.usecase.DeleteOwnerUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerDetailUseCase
import com.github.rodrigotimoteo.animally.domain.owner.usecase.GetOwnerListUseCase
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.patient.model.Patient
import com.github.rodrigotimoteo.animally.domain.patient.usecase.DeletePatientUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientDetailUseCase
import com.github.rodrigotimoteo.animally.domain.patient.usecase.GetPatientListUseCase
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetDentistryRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.reminder.usecase.GetVaccinationRemindersUseCase
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.usecase.GetReproductionEventsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.usecase.GetReproMedicationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.substance.usecase.GetControlledSubstancesByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.surgery.usecase.GetSurgeriesByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.usecase.GetUltrasoundsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.model.Vaccination
import com.github.rodrigotimoteo.animally.domain.vaccination.usecase.GetVaccinationsByPatientUseCase
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.domain.weight.model.Weight
import com.github.rodrigotimoteo.animally.domain.weight.usecase.GetWeightsByPatientUseCase
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.reminder.ReminderSettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.DesktopThemePreferenceStore
import com.github.rodrigotimoteo.animally.presentation.settings.SettingsViewModel
import com.github.rodrigotimoteo.animally.presentation.settings.ThemePreferenceStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Instant

/**
 * Shared fixtures for the desktop Compose UI tests.
 *
 * Installs an unconfined test dispatcher as [Dispatchers.Main].
 *
 * The headless Skiko test runtime has no platform Main dispatcher, and view model scopes
 * require one. Unconfined keeps launches eager so VM work completes without needing to
 * advance a second scheduler; the v2 test framework still owns the composition clock that
 * [androidx.compose.ui.test.ComposeUiTest.waitUntil] advances.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun installMainDispatcher() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
}

/** Restores the original Main dispatcher after a test. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun restoreMainDispatcher() {
    Dispatchers.resetMain()
}

/**
 * Koin modules wiring the real repos, use cases and view models over an in-memory database.
 *
 * The services are declared explicitly instead of relying on the Koin annotations generated
 * module, keeping resolution deterministic under incremental compilation.
 */
internal fun uiTestKoinModules(): List<Module> =
    buildList {
        addAll(databaseTestModules())
        add(uiServicesModule())
        addAll(uiViewModelModule())
    }

private fun uiServicesModule(): Module =
    module {
        single<AnimallyNavigator> { AnimallyNavigator() }
        single<CoroutineDispatcher>(named(IO_DISPATCHER)) { Dispatchers.IO }
        single<ThemePreferenceStore> { DesktopThemePreferenceStore() }

        single<IPatientRepository> { PatientRepositoryImpl(get()) }
        single<IOwnerRepository> { OwnerRepositoryImpl(get<AnimallyDatabase>().ownerQueries) }
        single<IAnamneseRepository> { AnamneseRepositoryImpl(get()) }
        single<IWeightRepository> { WeightRepositoryImpl(get()) }
        single<IVaccinationRepository> { VaccinationRepositoryImpl(get()) }
        single<IConsultationRepository> { ConsultationRepositoryImpl(get()) }
        single<ILamenessRepository> { LamenessRepositoryImpl(get()) }
        single<ISurgeryRepository> { SurgeryRepositoryImpl(get()) }
        single<IMedicationRepository> { MedicationRepositoryImpl(get()) }
        single<IControlledSubstanceRepository> { ControlledSubstanceRepositoryImpl(get()) }
        single<IDewormingRepository> { DewormingRepositoryImpl(get()) }
        single<IDentistryRepository> { DentistryRepositoryImpl(get()) }
        single<IFarrierVisitRepository> { FarrierVisitRepositoryImpl(get()) }
        single<IReproductionRepository> { ReproductionRepositoryImpl(get()) }
        single<IUltrasoundRepository> { UltrasoundRepositoryImpl(get()) }
        single<IGestationRepository> { GestationRepositoryImpl(get()) }
        single<IReproMedicationRepository> { ReproMedicationRepositoryImpl(get()) }
        single<ILabResultRepository> { LabResultRepositoryImpl(get()) }
        single<IImagingRepository> { ImagingRepositoryImpl(get()) }
        single<ISearchRepository> { SearchRepositoryImpl(get()) }

        single { GetPatientListUseCase(get()) }
        single { GetPatientDetailUseCase(get()) }
        single { DeletePatientUseCase(get(), get()) }
        single { GetOwnerListUseCase(get()) }
        single { GetOwnerDetailUseCase(get()) }
        single { DeleteOwnerUseCase(get(), get()) }
        single { GetWeightsByPatientUseCase(get()) }
        single { GetVaccinationsByPatientUseCase(get()) }
        single { GetConsultationsByPatientUseCase(get()) }
        single { GetLamenessListByPatientUseCase(get()) }
        single { GetSurgeriesByPatientUseCase(get()) }
        single { GetMedicationsByPatientUseCase(get()) }
        single { GetControlledSubstancesByPatientUseCase(get()) }
        single { GetDewormingsByPatientUseCase(get()) }
        single { GetDentistryListByPatientUseCase(get()) }
        single { GetFarrierVisitsByPatientUseCase(get()) }
        single { GetReproductionEventsByPatientUseCase(get()) }
        single { GetUltrasoundsByPatientUseCase(get()) }
        single { GetGestationsByPatientUseCase(get()) }
        single { GetReproMedicationsByPatientUseCase(get()) }
        single { GetLabResultsByPatientUseCase(get()) }
        single { GetImagingListByPatientUseCase(get()) }

        single { CsvExporter() }
        single { ExportBasicRecordsUseCase(get(), get(), get(), get(), get(), get()) }
        single { ExportClinicalRecordsUseCase(get(), get(), get(), get(), get(), get()) }
        single { ExportReproductiveRecordsUseCase(get(), get(), get(), get(), get()) }
        single { ExportCsvUseCase(get(), get(), get(), get(), get()) }
        single { ExportPatientReportUseCase(get(), get(), get(), get()) }
        single { ExportBackupUseCase(get()) }
        single { RestoreBackupUseCase(get()) }
        single { GetVaccinationRemindersUseCase(get(), get()) }
        single { GetDentistryRemindersUseCase(get(), get()) }
        single<NotificationPermissionController> { NotificationPermissionControllerImpl() }
    }

private fun uiViewModelModule(): List<Module> =
    module {
        viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get()) }
        viewModel {
            ReminderSettingsViewModel(get(), get(), get(named(IO_DISPATCHER)), get())
        }
    } + PresentationModule().provide()

/**
 * Supplies a [LocalLifecycleOwner] so screens collecting via `collectAsStateWithLifecycle`
 * work under the headless test composition. The lifecycle reports RESUMED immediately and
 * dispatches create/start/resume to every observer, which is all `repeatOnLifecycle` needs.
 */
@Composable
internal fun ProvideTestLifecycle(content: @Composable () -> Unit) {
    val owner = remember { TestLifecycleOwner() }
    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
        content()
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    private val lifecycleImpl = TestLifecycle(this)
    override val lifecycle: Lifecycle get() = lifecycleImpl
}

private class TestLifecycle(
    private val owner: TestLifecycleOwner,
) : Lifecycle() {
    private val observers = mutableListOf<LifecycleObserver>()

    override val currentState: Lifecycle.State get() = Lifecycle.State.RESUMED

    override fun addObserver(observer: LifecycleObserver) {
        observers += observer
        if (observer is LifecycleEventObserver) {
            val stateOwner = owner
            CoroutineScope(Dispatchers.Default).launch {
                observer.onStateChanged(stateOwner, Lifecycle.Event.ON_CREATE)
                observer.onStateChanged(stateOwner, Lifecycle.Event.ON_START)
                observer.onStateChanged(stateOwner, Lifecycle.Event.ON_RESUME)
            }
        }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers -= observer
    }
}

private fun epoch(): Instant = Instant.fromEpochMilliseconds(0L)

internal fun AnimallyDatabase.seedPatient(name: String = "Midnight"): Patient {
    val repository = PatientRepositoryImpl(this)
    val id =
        repository.insertPatient(
            Patient(
                id = 0L,
                name = name,
                species = "Equine",
                breed = "Lusitano",
                microchipId = "981000123456789",
                createdAt = epoch(),
                updatedAt = epoch(),
            ),
        )
    return repository.getPatientById(id) ?: error("Failed to seed patient")
}

internal fun AnimallyDatabase.seedVaccination(
    patientId: Long,
    vaccineName: String = "Tetanus",
    date: LocalDate = LocalDate(2024, 5, 1),
): Vaccination {
    val repository = VaccinationRepositoryImpl(this)
    val id =
        repository.insert(
            Vaccination(
                id = 0L,
                patientId = patientId,
                vaccineName = vaccineName,
                dateAdministered = date,
                createdAt = epoch(),
                updatedAt = epoch(),
            ),
        )
    return repository.getById(id) ?: error("Failed to seed vaccination")
}

internal fun AnimallyDatabase.seedWeight(
    patientId: Long,
    weightKg: Double = 520.0,
    date: LocalDate = LocalDate(2024, 5, 2),
): Weight {
    val repository = WeightRepositoryImpl(this)
    val id =
        repository.insert(
            Weight(
                id = 0L,
                patientId = patientId,
                weightKg = weightKg,
                date = date,
                createdAt = epoch(),
                updatedAt = epoch(),
            ),
        )
    return repository.getById(id) ?: error("Failed to seed weight")
}

internal fun AnimallyDatabase.seedOwner(name: String = "Alice Brown"): Owner {
    val repository = OwnerRepositoryImpl(ownerQueries)
    val id =
        repository.insertOwner(
            Owner(
                id = 0L,
                name = name,
                email = "alice@example.com",
                phone = null,
                address = null,
                createdAt = epoch(),
                updatedAt = epoch(),
            ),
        )
    return repository.getOwnerById(id) ?: error("Failed to seed owner")
}
