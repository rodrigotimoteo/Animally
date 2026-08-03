package com.github.rodrigotimoteo.animally.presentation.settings

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.anamnese.IAnamneseRepository
import com.github.rodrigotimoteo.animally.domain.backup.BACKUP_SCHEMA_VERSION
import com.github.rodrigotimoteo.animally.domain.backup.BackupPayload
import com.github.rodrigotimoteo.animally.domain.backup.BackupSerializer
import com.github.rodrigotimoteo.animally.domain.backup.ExportBackupUseCase
import com.github.rodrigotimoteo.animally.domain.backup.RestoreBackupUseCase
import com.github.rodrigotimoteo.animally.domain.consultation.IConsultationRepository
import com.github.rodrigotimoteo.animally.domain.dentistry.IDentistryRepository
import com.github.rodrigotimoteo.animally.domain.deworming.IDewormingRepository
import com.github.rodrigotimoteo.animally.domain.export.CsvExporter
import com.github.rodrigotimoteo.animally.domain.export.ExportBasicRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportClinicalRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportCsvUseCase
import com.github.rodrigotimoteo.animally.domain.export.ExportReproductiveRecordsUseCase
import com.github.rodrigotimoteo.animally.domain.export.pdf.ExportPatientReportUseCase
import com.github.rodrigotimoteo.animally.domain.farrier.IFarrierVisitRepository
import com.github.rodrigotimoteo.animally.domain.gestation.IGestationRepository
import com.github.rodrigotimoteo.animally.domain.imaging.IImagingRepository
import com.github.rodrigotimoteo.animally.domain.labresult.ILabResultRepository
import com.github.rodrigotimoteo.animally.domain.lameness.ILamenessRepository
import com.github.rodrigotimoteo.animally.domain.medication.IMedicationRepository
import com.github.rodrigotimoteo.animally.domain.patient.IPatientRepository
import com.github.rodrigotimoteo.animally.domain.reproduction.IReproductionRepository
import com.github.rodrigotimoteo.animally.domain.repromedication.IReproMedicationRepository
import com.github.rodrigotimoteo.animally.domain.substance.IControlledSubstanceRepository
import com.github.rodrigotimoteo.animally.domain.surgery.ISurgeryRepository
import com.github.rodrigotimoteo.animally.domain.ultrasound.IUltrasoundRepository
import com.github.rodrigotimoteo.animally.domain.vaccination.IVaccinationRepository
import com.github.rodrigotimoteo.animally.domain.weight.IWeightRepository
import com.github.rodrigotimoteo.animally.presentation.navigation.AnimallyNavigator
import com.github.rodrigotimoteo.animally.presentation.theme.ThemeMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the export, backup and restore actions of [SettingsViewModel].
 *
 * The share-sheet entry points (`shareFile`, `shareFileAt`, `sharePdf`) are
 * platform actuals that need an initialized Android context, so they are not
 * exercised on the host; this file covers every pure branch of the settings
 * actions. Theme behavior is covered by [SettingsViewModelThemeTest].
 */
class SettingsViewModelTest {
    private val patientRepositoryMock: IPatientRepository = mock()

    private val anamneseRepositoryMock: IAnamneseRepository = mock()

    private val weightRepositoryMock: IWeightRepository = mock()

    private val consultationRepositoryMock: IConsultationRepository = mock()

    private val vaccinationRepositoryMock: IVaccinationRepository = mock()

    private val dewormingRepositoryMock: IDewormingRepository = mock()

    private val dentistryRepositoryMock: IDentistryRepository = mock()

    private val lamenessRepositoryMock: ILamenessRepository = mock()

    private val surgeryRepositoryMock: ISurgeryRepository = mock()

    private val medicationRepositoryMock: IMedicationRepository = mock()

    private val labResultRepositoryMock: ILabResultRepository = mock()

    private val imagingRepositoryMock: IImagingRepository = mock()

    private val farrierRepositoryMock: IFarrierVisitRepository = mock()

    private val reproductionRepositoryMock: IReproductionRepository = mock()

    private val ultrasoundRepositoryMock: IUltrasoundRepository = mock()

    private val gestationRepositoryMock: IGestationRepository = mock()

    private val reproMedicationRepositoryMock: IReproMedicationRepository = mock()

    private val substanceRepositoryMock: IControlledSubstanceRepository = mock()

    private val basicRecords =
        ExportBasicRecordsUseCase(
            anamneseRepositoryMock,
            weightRepositoryMock,
            consultationRepositoryMock,
            vaccinationRepositoryMock,
            dewormingRepositoryMock,
            dentistryRepositoryMock,
        )

    private val clinicalRecords =
        ExportClinicalRecordsUseCase(
            lamenessRepositoryMock,
            surgeryRepositoryMock,
            medicationRepositoryMock,
            labResultRepositoryMock,
            imagingRepositoryMock,
            farrierRepositoryMock,
        )

    private val reproductiveRecords =
        ExportReproductiveRecordsUseCase(
            reproductionRepositoryMock,
            ultrasoundRepositoryMock,
            gestationRepositoryMock,
            reproMedicationRepositoryMock,
            substanceRepositoryMock,
        )

    private val exportCsvUseCase =
        ExportCsvUseCase(
            patientRepositoryMock,
            basicRecords,
            clinicalRecords,
            reproductiveRecords,
            CsvExporter(),
        )

    private val themePreferenceStore = SettingsFakeThemePreferenceStore()

    private val navigator = AnimallyNavigator()

    private lateinit var database: AnimallyDatabase

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
    }

    private fun createViewModel() =
        SettingsViewModel(
            exportCsvUseCase = exportCsvUseCase,
            exportBackupUseCase = ExportBackupUseCase(database),
            restoreBackupUseCase = RestoreBackupUseCase(database),
            exportReportUseCase =
                ExportPatientReportUseCase(
                    patientRepositoryMock,
                    basicRecords,
                    clinicalRecords,
                    reproductiveRecords,
                ),
            patientRepository = patientRepositoryMock,
            themePreferenceStore = themePreferenceStore,
            animallyNavigator = navigator,
        )

    private val emptyBackupJson =
        BackupSerializer.encode(
            BackupPayload(
                schemaVersion = BACKUP_SCHEMA_VERSION,
                exportedAt = "2026-01-01T00:00:00Z",
                patients = emptyList(),
                owners = emptyList(),
                anamnese = emptyList(),
                consultations = emptyList(),
                vaccinations = emptyList(),
                weights = emptyList(),
                dewormings = emptyList(),
                dentistry = emptyList(),
                lameness = emptyList(),
                surgeries = emptyList(),
                medications = emptyList(),
                labResults = emptyList(),
                imaging = emptyList(),
                farrierVisits = emptyList(),
                reproductionEvents = emptyList(),
                ultrasounds = emptyList(),
                gestations = emptyList(),
                reproMedications = emptyList(),
                substances = emptyList(),
            ),
        )

    @Test
    fun `restore with blank json shows paste prompt`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.restoreJson = "   "
        vm.onRestoreBackupClick()

        assertEquals("Paste backup JSON first", vm.restoreStatus)
    }

    @Test
    fun `restore with valid json reports restore complete`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.restoreJson = emptyBackupJson
        vm.onRestoreBackupClick()

        assertEquals("Restore complete", vm.restoreStatus)
    }

    @Test
    fun `restore with malformed json reports failure`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.restoreJson = "not-valid-json"
        vm.onRestoreBackupClick()

        assertTrue(vm.restoreStatus!!.startsWith("Restore failed:"))
    }

    @Test
    fun `selecting patient updates selected patient id`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.onSelectPatient(7L)

        assertEquals(7L, vm.selectedPatientId)
    }

    @Test
    fun `export pdf without selected patient shows prompt`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.onExportPdfClick()

        assertEquals("Select a patient first", vm.pdfStatus)
    }

    @Test
    fun `theme mode change persists and notifies observers`() {
        every { patientRepositoryMock.getPatientList() } returns emptyList()
        val vm = createViewModel()

        vm.onThemeModeChange(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, vm.themeMode.value)
        assertEquals(ThemeMode.DARK, themePreferenceStore.getThemeMode())
    }
}

/**
 * In-memory [ThemePreferenceStore] for tests.
 */
private class SettingsFakeThemePreferenceStore : ThemePreferenceStore {
    private var storedMode: ThemeMode = ThemeMode.SYSTEM

    override fun getThemeMode(): ThemeMode = storedMode

    override fun setThemeMode(mode: ThemeMode) {
        storedMode = mode
    }
}
