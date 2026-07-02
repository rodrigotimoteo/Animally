# Animally — Navigation & Libraries

## Screen Navigation Map

```
App
├── PatientList (Home)
│   ├── Search (Global)
│   └── PatientDetail
│       ├── Profile (Anamnese + Weight History)
│       ├── SOAP Notes
│       │   └── AddEditConsultation
│       ├── Vaccinations
│       │   └── AddEditVaccination
│       ├── Dentistry
│       │   └── AddEditDentistry
│       ├── Lameness Evaluations
│       │   └── AddEditLameness
│       ├── Surgeries
│       │   └── AddEditSurgery
│       ├── Medications
│       │   └── AddEditMedication
│       ├── Lab Results
│       │   └── AddEditLabResult
│       ├── Imaging
│       │   └── AddEditImaging
│       ├── Farrier Visits
│       │   └── AddEditFarrier
│       ├── Reproduction
│       │   ├── Reproduction Events
│       │   │   └── AddEditReproductionEvent
│       │   ├── Ultrasounds
│       │   │   └── AddEditUltrasound
│       │   ├── Gestation Tracker
│       │   │   └── AddEditGestation
│       │   └── Reproduction Meds
│       │       └── AddEditReproMed
│       ├── Controlled Substances
│       │   └── AddEditControlled
│       ├── Files & Attachments
│       └── Export
├── OwnerList
│   └── OwnerDetail
│       └── (linked horses → PatientDetail)
└── Settings
    ├── Backup/Restore
    └── Reminders
```

## Routes (Navigation 3)

Navigation 3 uses `@Serializable` sealed classes as route definitions. Polymorphic sealing keeps the type hierarchy clean across platforms.

```kotlin
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {

    @Serializable data object PatientList : Route
    @Serializable data object OwnerList : Route
    @Serializable data class Search(val query: String = "") : Route
    @Serializable data object Settings : Route

    @Serializable data class PatientDetail(val patientId: Long) : Route

    // Add/Edit screens — null id = create new, non-null = edit existing
    @Serializable data class AddEditConsultation(val patientId: Long, val consultationId: Long? = null) : Route
    @Serializable data class AddEditVaccination(val patientId: Long, val vaccinationId: Long? = null) : Route
    @Serializable data class AddEditDentistry(val patientId: Long, val dentistryId: Long? = null) : Route
    @Serializable data class AddEditLameness(val patientId: Long, val lamenessId: Long? = null) : Route
    @Serializable data class AddEditSurgery(val patientId: Long, val surgeryId: Long? = null) : Route
    @Serializable data class AddEditMedication(val patientId: Long, val medicationId: Long? = null) : Route
    @Serializable data class AddEditLabResult(val patientId: Long, val labResultId: Long? = null) : Route
    @Serializable data class AddEditImaging(val patientId: Long, val imagingId: Long? = null) : Route
    @Serializable data class AddEditFarrier(val patientId: Long, val farrierId: Long? = null) : Route
    @Serializable data class AddEditReproductionEvent(val patientId: Long, val eventId: Long? = null) : Route
    @Serializable data class AddEditUltrasound(val patientId: Long, val ultrasoundId: Long? = null) : Route
    @Serializable data class AddEditGestation(val patientId: Long, val gestationId: Long? = null) : Route
    @Serializable data class AddEditReproMed(val patientId: Long, val reproMedId: Long? = null) : Route
    @Serializable data class AddEditControlledSubstance(val patientId: Long, val controlledId: Long? = null) : Route

    @Serializable data class OwnerDetail(val ownerId: Long) : Route
    @Serializable data class PatientFiles(val patientId: Long) : Route
    @Serializable data class PatientExport(val patientId: Long) : Route
}
```

### Polymorphic Serialization Setup

For iOS (non-JVM target), you need a `SerializersModule` to register route subtypes:

```kotlin
// commonMain
val routeSerializersModule = SerializersModule {
    polymorphic(Route::class) {
        subclass(Route.PatientList::class)
        subclass(Route.PatientDetail::class)
        subclass(Route.AddEditConsultation::class)
        // ... all route subclasses
    }
}
```

### NavHost Setup

```kotlin
@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(
        startDestination = Route.PatientList,
        savedStateConfiguration = SavedStateConfiguration(
            serializersModule = routeSerializersModule
        )
    )

    NavHost(
        backStack = backStack,
        savedStateConfiguration = SavedStateConfiguration(
            serializersModule = routeSerializersModule
        )
    ) { backStackEntry ->
        when (backStackEntry.destination.route) {
            is Route.PatientList -> PatientListScreen(backStack)
            is Route.PatientDetail -> PatientDetailScreen(backStackEntry, backStack)
            is Route.AddEditConsultation -> AddEditConsultationScreen(backStackEntry, backStack)
            // ... all routes
        }
    }
}
```

## Recommended Libraries

### Core
| Library | Purpose |
|---------|---------|
| **Compose Multiplatform** | Shared UI across Android + iOS |
| **SQLDelight** | Local database with type-safe Kotlin APIs, FTS5 support |
| **Koin** | Dependency injection — only KMP-ready DI framework. Hilt is Android-only, no KMP support. |
| **Koin Annotations** | Compile-time DI via KSP. Same style as Hilt (`@Single`, `@Factory`, `@Module`). KMP-ready. |
| **Koin Compiler Plugin** | K2 native plugin, auto-wires constructors, validates graph at compile time. Recommended for Kotlin 2.x. |
| **Kotlinx Serialization** | JSON serialization for export/backup |
| **Kotlinx Datetime** | Date/time handling across platforms |

### Navigation
| Library | Purpose |
|---------|---------|
| **Navigation 3** | Modern KMP navigation from JetBrains. Type-safe sealed route classes, direct back stack manipulation, multiplatform Android/iOS/Desktop/Web. |

### Storage & Files
| Library | Purpose |
|---------|---------|
| **SQLDelight** | Structured data (all tables above) |
| **KMP-Files** or **OKIO** | Cross-platform file system access for attachments |
| **Coil (Compose)** | Image loading and thumbnails for attached images |

### PDF & Export
| Library | Purpose |
|---------|---------|
| **iText / OpenPDF** | PDF generation for patient reports |
| **Apache POI** or manual CSV | CSV export for spreadsheet analysis |

### UI & UX
| Library | Purpose |
|---------|---------|
| **Haze** | Hardware blur / glass effects (Phase 5) |
| **Material 3** | Design system (comes with Compose Multiplatform) |
| **Accompanist Permissions** | Runtime permission handling (camera, storage) |

### Notifications
| Library | Purpose |
|---------|---------|
| **KMP-Notifier** | Cross-platform local notifications for reminders |

### Networking (Phase 6)
| Library | Purpose |
|---------|---------|
| **Ktor Client** | HTTP client for future Postgres backend |
| **Ktor Server** | Backend server (if building sync) |

### Image Capture
| Library | Purpose |
|---------|---------|
| **CameraX (Android) / NSKoala (iOS)** | Camera integration for photo attachments |

## Start Here

Phase 1 needs:
1. Compose Multiplatform (UI)
2. SQLDelight (database)
3. Koin (DI)
4. Navigation 3 (navigation)
5. Kotlinx Serialization + Datetime (data)

Everything else comes in later phases. Don't add libraries you won't use yet.

## Project Structure

### Gradle Modules

```
Animally/
├── shared/                    # Shared KMP module (where 90% of code lives)
│   ├── commonMain/            # Shared code for all platforms
│   │   ├── domain/            # Business logic
│   │   ├── data/              # Database, repositories, API
│   │   └── presentation/      # Compose UI, ViewModels
│   ├── androidMain/           # Android-specific implementations
│   ├── iosMain/               # iOS-specific implementations
│   └── build.gradle.kts
├── androidApp/                # Android app entry point only
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/.../MainActivity.kt
├── iosApp/                    # iOS app entry point only
│   └── iOSApp.swift
└── build.gradle.kts
```

### What Goes Where

#### `commonMain` — shared/domain/

Pure Kotlin, no platform dependencies. Business rules that work identically on Android and iOS.

```
domain/
├── model/                     # Data classes
│   ├── Patient.kt
│   ├── Owner.kt
│   ├── Consultation.kt
│   ├── Vaccination.kt
│   ├── Dentistry.kt
│   ├── LamenessEvaluation.kt
│   ├── Surgery.kt
│   ├── MedicationLog.kt
│   ├── LabResult.kt
│   ├── Imaging.kt
│   ├── FarrierVisit.kt
│   ├── Reproduction.kt
│   ├── Ultrasound.kt
│   ├── Gestation.kt
│   └── ControlledSubstance.kt
├── repository/                # Interfaces only
│   ├── PatientRepository.kt
│   ├── OwnerRepository.kt
│   └── ...
└── usecase/                   # Business logic
    ├── GetPatientHistory.kt
    ├── CalculateNextVaccination.kt
    ├── CalculateGestationDueDate.kt
    ├── SearchRecords.kt
    └── ExportPatientReport.kt
```

**Rules:**
- Models = data classes with `@Serializable`
- Repositories = interfaces (implementations live in data/)
- Use cases = classes that orchestrate repository calls
- No `expect`/`actual` here — pure Kotlin

#### `commonMain` — shared/data/

Repository implementations, database, file handling. Platform-specific parts use `expect`/`actual`.

```
data/
├── database/                  # SQLDelight
│   ├── AnimallyDatabase.kt   # Database driver (expect/actual)
│   ├── PatientDao.kt
│   ├── ConsultationDao.kt
│   └── migrations/
├── repository/                # Implements domain interfaces
│   ├── PatientRepositoryImpl.kt
│   ├── OwnerRepositoryImpl.kt
│   └── ...
├── di/                        # Koin modules
│   ├── DomainModule.kt
│   ├── DataModule.kt
│   └── DatabaseModule.kt
└── file/                      # File storage (expect/actual)
    └── FileStorage.kt
```

**`expect`/`actual` examples:**
```kotlin
// commonMain
expect fun createDatabaseDriver(): SqlDriver

// androidMain
actual fun createDatabaseDriver(): SqlDriver =
    AndroidSqliteDriver(Animally.Schema, context, "animally.db")

// iosMain
actual fun createDatabaseDriver(): SqlDriver =
    NativeSqliteDriver(Animally.Schema, "animally.db")
```

#### `commonMain` — shared/presentation/

Compose Multiplatform UI + ViewModels. Shared across platforms.

```
presentation/
├── components/                # Reusable composables
│   ├── PatientCard.kt
│   ├── RecordListItem.kt
│   ├── SearchBar.kt
│   └── ...
├── screen/                    # Screen composables
│   ├── patientlist/
│   │   └── PatientListScreen.kt
│   ├── patientdetail/
│   │   ├── PatientDetailScreen.kt
│   │   ├── PatientDetailViewModel.kt
│   │   └── sections/
│   │       ├── ProfileSection.kt
│   │       ├── SoapNotesSection.kt
│   │       ├── VaccinationsSection.kt
│   │       └── ...
│   ├── addedit/
│   │   ├── AddEditConsultationScreen.kt
│   │   ├── AddEditVaccinationScreen.kt
│   │   └── ...
│   ├── owner/
│   │   ├── OwnerListScreen.kt
│   │   └── OwnerDetailScreen.kt
│   └── settings/
│       └── SettingsScreen.kt
└── navigation/                # Navigation 3 routes + NavHost
    ├── Routes.kt              # Sealed route definitions
    ├── Serializers.kt         # Polymorphic serializers module
    └── AppNavHost.kt          # NavHost with all screen mappings
```

**Rules:**
- ViewModels use `@KoinViewModel` annotation
- Screens are `@Composable` functions
- No Android/iOS imports here — use only Compose Multiplatform APIs

---

#### `androidMain` — what stays here

Only Android-specific code. Minimal.

```
androidMain/
├── AnimallyApp.kt             # Application class (@HiltAndroidApp or Koin init)
├── MainActivity.kt            # In androidApp module, not shared
├── platform/                  # expect/actual implementations
│   ├── DatabaseDriver.kt      # actual fun createDatabaseDriver()
│   ├── FileStorage.kt         # actual class FileStorage
│   ├── Notifications.kt       # Android notification channel setup
│   └── Camera.kt              # CameraX integration
└── di/
    └── PlatformModule.kt      # Android-specific Koin bindings
```

**Only Android things:**
- `Application` class
- `Activity` / `MainActivity`
- Notification channel creation (Android-specific)
- CameraX integration
- Android file system paths (`Context.filesDir`)
- WorkManager for background reminders

#### `iosMain` — what stays here

Only iOS-specific code. Minimal.

```
iosMain/
├── platform/                  # expect/actual implementations
│   ├── DatabaseDriver.kt      # actual fun createDatabaseDriver()
│   ├── FileStorage.kt         # actual class FileStorage
│   ├── Notifications.kt       # UNUserNotificationCenter
│   └── Camera.kt              # AVFoundation
└── di/
    └── PlatformModule.kt      # iOS-specific Koin bindings
```

**Only iOS things:**
- Koin initialization in `iOSApp.swift`
- `UNUserNotificationCenter` for reminders
- AVFoundation for camera
- iOS file system paths (`NSDocumentDirectory`)

#### `androidApp/` — app shell only

Just the entry point. No business logic.

```kotlin
// androidApp/src/main/java/.../MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnimallyApp()  // Calls shared composable
        }
    }
}
```

#### `iosApp/` — app shell only

Just the SwiftUI entry point. No business logic.

```swift
// iosApp/iOSApp.swift
@main
struct iOSApp: App {
    init() {
        KoinHelperKt.doInitKoin()  // Start shared Koin
    }

    var body: some Scene {
        WindowGroup {
            ContentView()  // Wraps shared Compose UI
        }
    }
}
```

### The 90/10 Rule

| Layer | Shared (commonMain) | Platform-specific |
|-------|-------------------|-------------------|
| Domain models | ✅ 100% | ❌ |
| Repository interfaces | ✅ 100% | ❌ |
| Use cases | ✅ 100% | ❌ |
| Database driver | ❌ | ✅ expect/actual |
| Repository impls | ✅ 90% | ❌ (file paths differ) |
| ViewModels | ✅ 100% | ❌ |
| Compose UI | ✅ 95% | ❌ (permissions differ) |
| Navigation | ✅ 100% | ❌ |
| Notifications | ❌ | ✅ expect/actual |
| Camera | ❌ | ✅ expect/actual |
| DI setup | ✅ modules | ✅ platform init |

**90% of code lives in commonMain.** Platform modules are thin wrappers for system APIs only.
