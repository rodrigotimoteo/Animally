# Animally — File Map + Model Schemas

Full file tree for every layer and every model's data fields.

---

## Cross-cutting

```
domain/common/
├── model/Identifiable.kt     # interface { val id: Long }
└── util/Result.kt            # sealed: Success/Error, or kotlin.Result

data/
├── di/
│   ├── DomainModule.kt       # Koin: bind repos (interface → impl)
│   ├── DataModule.kt         # Koin: DB driver, generated queries, file
│   └── DatabaseModule.kt     # Koin: SQLDelight driver singleton
├── file/
│   └── FileStorage.kt        # expect/actual for platform file system
├── database/
│   ├── AnimallyDatabase.kt   # expect fun createDatabaseDriver()
│   └── migrations/           # SQLDelight .sqm files

presentation/
├── theme/
│   ├── Theme.kt              # Material3 theme
│   ├── Color.kt              # Color tokens
│   └── Type.kt               # Typography
├── components/
│   ├── PatientCard.kt        # Reusable card for list items
│   ├── RecordListItem.kt     # Generic record row (title, subtitle, date)
│   ├── SearchBar.kt          # Global search input
│   └── EmptyState.kt         # "No data" placeholder
├── navigation/
│   ├── Routes.kt             # sealed interface Route { ... }
│   ├── Serializers.kt        # SerializersModule polymorphic(Route::class)
│   └── AppNavHost.kt         # NavHost + when(route) dispatch
└── app/
    └── AnimallyApp.kt        # @Composable root: KoinApplication + Theme + NavHost
```

---

## Patient

**Files:**
```
domain/patient/
├── model/Patient.kt
├── repository/PatientRepository.kt
└── usecase/
    ├── GetPatientListUseCase.kt
    ├── GetPatientDetailUseCase.kt
    ├── SearchPatientsUseCase.kt
    └── ExportPatientReportUseCase.kt

data/patient/
├── Patient.sq
└── PatientRepositoryImpl.kt

presentation/patient/
├── PatientListScreen.kt
├── PatientListViewModel.kt
├── PatientDetailScreen.kt
└── PatientDetailViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Patient(
    val id: Long,
    val name: String,
    val species: String,              // "Equine", "Bovine", etc.
    val breed: String,
    val dateOfBirth: LocalDate,
    val color: String,                // coat color / markings
    val gender: String,               // "Mare", "Stallion", "Gelding"
    val microchipId: String? = null,
    val ueln: String? = null,         // Unique Equine Life Number — 15-digit ISO, linked to microchip
    val registrationNumber: String? = null,  // Studbook/federation registration
    val ownerId: Long,
    val stableLocation: String? = null,
    val photoUri: String? = null,
    val cogginsTestDate: LocalDate? = null,
    val cogginsResult: String? = null,        // "Positive", "Negative"
    val cogginsExpiryDate: LocalDate? = null,
    val notes: String? = null,
    val isActive: Boolean = true,             // Soft delete — deactivate, never hard-delete
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Anamnese

1:1 with Patient. General medical history, chronic conditions, allergies. Distinct from consultation-specific SOAP notes.

**Files:**
```
domain/anamnese/
├── model/Anamnese.kt
├── repository/AnamneseRepository.kt
└── usecase/
    └── GetAnamneseUseCase.kt

data/anamnese/
├── Anamnese.sq
└── AnamneseRepositoryImpl.kt

presentation/anamnese/
├── AddEditAnamneseScreen.kt
└── AddEditAnamneseViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Anamnese(
    val id: Long,
    val patientId: Long,              // 1:1 with Patient
    val generalHistory: String,
    val chronicConditions: String,
    val allergies: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Weight Entry

Historical weight measurements for a Patient. Tracked over time, not as a single value on Patient.

**Files:**
```
domain/weight/
├── model/WeightEntry.kt
├── repository/WeightRepository.kt
└── usecase/
    └── GetWeightHistoryUseCase.kt

data/weight/
├── Weight.sq
└── WeightRepositoryImpl.kt

presentation/weight/
├── AddEditWeightScreen.kt
└── AddEditWeightViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class WeightEntry(
    val id: Long,
    val patientId: Long,
    val weight: Double,               // kg
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Owner

**Files:**
```
domain/owner/
├── model/Owner.kt
├── repository/OwnerRepository.kt
└── usecase/
    ├── GetOwnerListUseCase.kt
    └── GetOwnerDetailUseCase.kt

data/owner/
├── Owner.sq
└── OwnerRepositoryImpl.kt

presentation/owner/
├── OwnerListScreen.kt
├── OwnerListViewModel.kt
├── OwnerDetailScreen.kt
└── OwnerDetailViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Owner(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Consultation (SOAP Notes)

**Files:**
```
domain/consultation/
├── model/Consultation.kt
├── repository/ConsultationRepository.kt
└── usecase/
    └── GetConsultationHistoryUseCase.kt

data/consultation/
├── Consultation.sq
└── ConsultationRepositoryImpl.kt

presentation/consultation/
├── AddEditConsultationScreen.kt
└── AddEditConsultationViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Consultation(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val subjective: String,           // "S" — owner's description
    val objective: String,            // "O" — examination findings
    val assessment: String,           // "A" — diagnosis
    val plan: String,                 // "P" — treatment plan
    val veterinarian: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Vaccination

**Files:**
```
domain/vaccination/
├── model/Vaccination.kt
├── repository/VaccinationRepository.kt
└── usecase/
    └── CalculateNextVaccinationUseCase.kt

data/vaccination/
├── Vaccination.sq
└── VaccinationRepositoryImpl.kt

presentation/vaccination/
├── AddEditVaccinationScreen.kt
└── AddEditVaccinationViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Vaccination(
    val id: Long,
    val patientId: Long,
    val vaccineName: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val manufacturer: String? = null,
    val batchNumber: String? = null,
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Deworming

Separate entity from Vaccination — different drug class, different reminder cadence.

**Files:**
```
domain/deworming/
├── model/Deworming.kt
├── repository/DewormingRepository.kt
└── usecase/
    └── CalculateNextDewormingUseCase.kt

data/deworming/
├── Deworming.sq
└── DewormingRepositoryImpl.kt

presentation/deworming/
├── AddEditDewormingScreen.kt
└── AddEditDewormingViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Deworming(
    val id: Long,
    val patientId: Long,
    val drugName: String,
    val dateAdministered: LocalDate,
    val nextDueDate: LocalDate? = null,
    val dosage: String? = null,
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Dentistry

**Files:**
```
domain/dentistry/
├── model/Dentistry.kt
├── repository/DentistryRepository.kt
└── usecase/
    └── GetDentistryHistoryUseCase.kt

data/dentistry/
├── Dentistry.sq
└── DentistryRepositoryImpl.kt

presentation/dentistry/
├── AddEditDentistryScreen.kt
└── AddEditDentistryViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Dentistry(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val findings: String,
    val treatment: String,
    val nextDueDate: LocalDate? = null,
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Lameness Evaluation

**Files:**
```
domain/lameness/
├── model/LamenessEvaluation.kt
├── repository/LamenessRepository.kt
└── usecase/
    └── GetLamenessHistoryUseCase.kt

data/lameness/
├── Lameness.sq
└── LamenessRepositoryImpl.kt

presentation/lameness/
├── AddEditLamenessScreen.kt
└── AddEditLamenessViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class LamenessEvaluation(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val grade: Int,                   // 1-5 scale (AAEP)
    val limb: String,                 // "LF", "RF", "LH", "RH", "Multiple"
    val gait: String? = null,         // "Walk", "Trot", "Canter"
    val surface: String? = null,      // "Hard", "Soft", "Circle"
    val findings: String,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Surgery

**Files:**
```
domain/surgery/
├── model/Surgery.kt
├── repository/SurgeryRepository.kt
└── usecase/
    └── GetSurgeryHistoryUseCase.kt

data/surgery/
├── Surgery.sq
└── SurgeryRepositoryImpl.kt

presentation/surgery/
├── AddEditSurgeryScreen.kt
└── AddEditSurgeryViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Surgery(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val procedureName: String,
    val description: String,
    val surgeon: String? = null,
    val anesthesiaProtocol: String? = null,
    val complications: String? = null,
    val followUpDate: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Medication

**Files:**
```
domain/medication/
├── model/MedicationLog.kt
├── repository/MedicationRepository.kt
└── usecase/
    └── GetMedicationHistoryUseCase.kt

data/medication/
├── Medication.sq
└── MedicationRepositoryImpl.kt

presentation/medication/
├── AddEditMedicationScreen.kt
└── AddEditMedicationViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class MedicationLog(
    val id: Long,
    val patientId: Long,
    val medicationName: String,
    val dosage: String,               // e.g. "5 mg", "10 mL"
    val route: String,                // "Oral", "IV", "IM", "Topical"
    val frequency: String,            // "BID", "SID", "PRN"
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val prescribedBy: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Lab Result

**Files:**
```
domain/labresult/
├── model/LabResult.kt
├── repository/LabResultRepository.kt
└── usecase/
    └── GetLabResultHistoryUseCase.kt

data/labresult/
├── LabResult.sq
└── LabResultRepositoryImpl.kt

presentation/labresult/
├── AddEditLabResultScreen.kt
└── AddEditLabResultViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class LabResult(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val testType: String,             // "Blood Count", "Chemistry", "Cultures"
    val result: String,
    val referenceRange: String? = null,
    val laboratory: String? = null,
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Imaging

**Files:**
```
domain/imaging/
├── model/Imaging.kt
├── repository/ImagingRepository.kt
└── usecase/
    └── GetImagingHistoryUseCase.kt

data/imaging/
├── Imaging.sq
└── ImagingRepositoryImpl.kt

presentation/imaging/
├── AddEditImagingScreen.kt
└── AddEditImagingViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Imaging(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val type: String,                 // "X-Ray", "Ultrasound", "MRI", "CT"
    val bodyPart: String,
    val findings: String,
    val imageUris: List<String> = emptyList(),
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Farrier Visit

**Files:**
```
domain/farrier/
├── model/FarrierVisit.kt
├── repository/FarrierRepository.kt
└── usecase/
    └── GetFarrierHistoryUseCase.kt

data/farrier/
├── Farrier.sq
└── FarrierRepositoryImpl.kt

presentation/farrier/
├── AddEditFarrierScreen.kt
└── AddEditFarrierViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class FarrierVisit(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val farrierName: String? = null,
    val trimType: String,             // "Full", "Touch-up"
    val shoeType: String? = null,     // "Steel", "Aluminum", "Egg-bar"
    val shoesApplied: Boolean = false,
    val frontShoes: Boolean? = null,
    val hindShoes: Boolean? = null,
    val notes: String? = null,
    val nextDueDate: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Reproduction Event

**Files:**
```
domain/reproduction/
├── model/ReproductionEvent.kt
├── repository/ReproductionRepository.kt
└── usecase/
    └── GetReproductionHistoryUseCase.kt

data/reproduction/
├── Reproduction.sq
└── ReproductionRepositoryImpl.kt

presentation/reproduction/
├── AddEditReproductionEventScreen.kt
└── AddEditReproductionEventViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class ReproductionEvent(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val eventType: String,            // "Heat", "Breeding", "Pregnancy Check", "Foaling"
    val description: String,
    val stallion: String? = null,
    val result: String? = null,       // "Positive", "Negative", "Live Foal"
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Ultrasound (Reproductive)

Reproductive ultrasound with structured fields. Distinct from diagnostic imaging.

**Files:**
```
domain/ultrasound/
├── model/Ultrasound.kt
├── repository/UltrasoundRepository.kt
└── usecase/
    └── GetUltrasoundHistoryUseCase.kt

data/ultrasound/
├── Ultrasound.sq
└── UltrasoundRepositoryImpl.kt

presentation/ultrasound/
├── AddEditUltrasoundScreen.kt
└── AddEditUltrasoundViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Ultrasound(
    val id: Long,
    val patientId: Long,
    val date: LocalDate,
    val ovaryStatus: String,          // Repro-specific: ovary status
    val uterineStatus: String,        // Repro-specific: uterine status
    val follicleSizeMm: Double? = null,  // Repro-specific: follicle size in mm
    val findings: String,
    val imageUris: List<String> = emptyList(),
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Gestation Tracker

**Files:**
```
domain/gestation/
├── model/Gestation.kt
├── repository/GestationRepository.kt
└── usecase/
    └── CalculateGestationDueDateUseCase.kt

data/gestation/
├── Gestation.sq
└── GestationRepositoryImpl.kt

presentation/gestation/
├── AddEditGestationScreen.kt
└── AddEditGestationViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class Gestation(
    val id: Long,
    val patientId: Long,
    val breedingDate: LocalDate,
    val expectedDueDate: LocalDate,
    val gestationDays: Int,           // Computed: days since breedingDate (recalculated on read)
    val status: String,               // "Confirmed", "Ongoing", "Completed", "Lost"
    val lastCheckDate: LocalDate? = null,
    val fetalCount: Int? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

`gestationDays` is computed from `breedingDate` to current date. Standard equine gestation ~340 days. Use case `CalculateGestationDueDateUseCase` computes `expectedDueDate` (breedingDate + 340) and `gestationDays` (today - breedingDate).

---

## Reproduction Meds

**Files:**
```
domain/repro-med/
├── model/ReproMed.kt
├── repository/ReproMedRepository.kt
└── usecase/
    └── GetReproMedHistoryUseCase.kt

data/repro-med/
├── ReproMed.sq
└── ReproMedRepositoryImpl.kt

presentation/repro-med/
├── AddEditReproMedScreen.kt
└── AddEditReproMedViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class ReproMed(
    val id: Long,
    val patientId: Long,
    val medicationName: String,
    val dosage: String,
    val administrationDate: LocalDate,
    val purpose: String,              // "Estrus Synchronization", "Ovulation Induction"
    val veterinarian: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Controlled Substance

**Files:**
```
domain/controlled-substance/
├── model/ControlledSubstance.kt
├── repository/ControlledSubstanceRepository.kt
└── usecase/
    └── GetControlledSubstanceHistoryUseCase.kt

data/controlled-substance/
├── ControlledSubstance.sq
└── ControlledSubstanceRepositoryImpl.kt

presentation/controlled-substance/
├── AddEditControlledSubstanceScreen.kt
└── AddEditControlledSubstanceViewModel.kt
```

**Model fields:**
```kotlin
@Serializable
data class ControlledSubstance(
    val id: Long,
    val patientId: Long,
    val substanceName: String,
    val quantity: Double,
    val unit: String,                 // "mg", "mL", "tablets"
    val administrationDate: LocalDate,
    val administeredBy: String,
    val witness: String? = null,      // Second professional for controlled substances
    val purpose: String,
    val lotNumber: String? = null,
    val expirationDate: LocalDate? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

---

## Settings

**Files:**
```
domain/settings/
├── model/                          # (none — no domain models)
└── usecase/                        # (none)

data/settings/                      # (none — platform preferences or config)

presentation/settings/
├── SettingsScreen.kt
├── BackupRestoreScreen.kt          # Phase 3
└── RemindersScreen.kt              # Phase 4
```

Settings use platform persistence (DataStore, NSUserDefaults) — no domain model or SQL needed.

---

## Summary

| Entity | Domain files | Data files | Presentation files | Fields |
|--------|-------------|-----------|-------------------|--------|
| Patient | 6 | 2 | 4 | 21 |
| Owner | 4 | 2 | 4 | 8 |
| Anamnese | 3 | 2 | 2 | 7 |
| Weight Entry | 3 | 2 | 2 | 7 |
| Consultation | 3 | 2 | 2 | 10 |
| Vaccination | 3 | 2 | 2 | 11 |
| Deworming | 3 | 2 | 2 | 10 |
| Dentistry | 3 | 2 | 2 | 10 |
| Lameness | 3 | 2 | 2 | 13 |
| Surgery | 3 | 2 | 2 | 11 |
| Medication | 3 | 2 | 2 | 12 |
| Lab Result | 3 | 2 | 2 | 10 |
| Imaging | 3 | 2 | 2 | 10 |
| Farrier | 3 | 2 | 2 | 12 |
| Reproduction | 3 | 2 | 2 | 11 |
| Ultrasound | 3 | 2 | 2 | 12 |
| Gestation | 3 | 2 | 2 | 11 |
| Repro Meds | 3 | 2 | 2 | 10 |
| Controlled Substance | 3 | 2 | 2 | 14 |

**Total: 19 entities.** Phase 1a (core): Patient, Owner, Anamnese, Consultation, Vaccination. Phase 1b (remaining): Weight Entry, Deworming, Dentistry, Lameness, Surgery, Medication, Lab Result, Imaging, Farrier, Reproduction, Ultrasound, Gestation, Repro Meds, Controlled Substance.
