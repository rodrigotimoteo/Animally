# Codebase Structure

## Directory Layout

```
Animally/
├── androidApp/              # Android application module
│   └── src/main/kotlin/...  # MainActivity, AnimallyApplication
├── iosApp/                  # iOS application module
│   └── iosApp/              # iOSApp.swift, ContentView.swift, native SwiftUI screens
├── shared/                  # KMP shared module (all business logic + UI)
│   └── src/
│       ├── commonMain/      # Cross-platform code
│       │   ├── kotlin/.../  # Source code
│       │   └── sqldelight/  # SQLDelight .sq schema files
│       ├── androidMain/     # Android-specific implementations
│       ├── iosMain/         # iOS-specific implementations
│       ├── nativeMain/      # Shared native (Apple) implementations
│       ├── commonTest/      # Cross-platform tests
│       ├── androidHostTest/ # Android JVM host tests
│       ├── desktopTest/     # Headless Compose UI tests (desktop target)
│       └── iosTest/         # iOS simulator tests
├── config/detekt/           # Detekt static analysis config
├── docs/adr/                # Architecture Decision Records
├── build.gradle.kts         # Root build config + detekt plugin + Kover
└── settings.gradle.kts      # Module includes
```

Root-level markdown also maintained: `CONTEXT.md` (domain glossary), `data.md`, `navigation.md`, `idea.md`, `plan.md`.

## Module Purposes

**`androidApp/`:**
- Purpose: Android application entry point. Thin shell around shared module.
- Contains: `MainActivity.kt`, `AnimallyApplication.kt`, Android resources (drawables, colors, themes)
- Key files:
  - `androidApp/src/main/kotlin/.../MainActivity.kt`: Single Activity hosting Compose UI
  - `androidApp/src/main/kotlin/.../AnimallyApplication.kt`: Application class, calls `initKoin(this)` on startup

**`iosApp/`:**
- Purpose: iOS application entry point. Thin shell wrapping shared Compose UI.
- Contains: `iOSApp.swift`, `ContentView.swift`, Xcode project config, asset catalog
- Key files:
  - `iosApp/iosApp/iOSApp.swift`: SwiftUI `@main` entry
  - `iosApp/iosApp/ContentView.swift`: Wraps `MainViewControllerKt.MainViewController()` in `UIViewControllerRepresentable`

**`shared/`:**
- Purpose: All shared business logic, data layer, UI, and platform `expect` declarations.
- Source sets:
  - `commonMain`: Pure Kotlin code — domain models, SQLDelight queries, Compose UI screens, ViewModels, DI modules, navigation
  - `androidMain`: `actual` implementations (Android `SqlDriver`, coroutine dispatchers, notifications, file/backup storage, PDF, theme prefs)
  - `iosMain`: `actual` implementations (iOS `NativeSqliteDriver`, coroutine dispatchers, notifications, file/backup storage, PDF, Swift store bridges)
  - `nativeMain`: Shared Apple-target `actual` implementations (empty currently)
  - `commonTest`: Multiplatform unit tests
  - `androidHostTest`: Android host JVM tests
  - `desktopTest`: Headless Compose UI tests (Compose UI test suite on desktop target)
  - `iosTest`: iOS simulator tests

## Shared Module Internal Layout

```
shared/src/commonMain/kotlin/.../
├── Platform.kt                    # expect fun getPlatform()
├── bridge/                        # Kotlin↔Swift bridge (NativeFlow, NativeCancellable, ObjCHidden)
├── data/
│   ├── adapters/                  # ColumnAdapter for Instant, LocalDate
│   │   ├── InstantAdapter.kt
│   │   └── LocalDateAdapter.kt
│   ├── <entity>/                  # one per entity (owner, patient, anamnese, ...)
│   │   ├── <Entity>RepositoryImpl.kt  # @Single impl
│   │   └── mapper/                # DTO → domain model mappers
│   ├── search/                    # FTS5 SearchRepository
│   ├── sync/                      # KtorSyncApi, SyncEngineImpl, SyncChangeTrackerImpl, SyncMetadataRepositoryImpl
│   ├── storage/                   # FileStorage, BackupStorage, PickedFile
│   ├── backup/                    # BackupRestore* impls
├── di/
│   ├── database/
│   │   ├── AnimallyDatabaseFactory.kt  # Factory creating DB with adapters
│   │   └── QueriesModule.kt            # Binds all *Queries singletons
│   ├── dispatchers/
│   │   ├── Dispatchers.kt         # expect declarations (io, main, default)
│   │   └── DispatchersModule.kt   # @Module providing named dispatchers
│   ├── http/
│   │   └── HttpClientModule.kt    # Ktor HTTP client for sync
│   ├── infra/
│   │   ├── AppModule.kt           # @Module @ComponentScan for DI
│   │   └── Initialize.kt          # expect fun initKoin()
│   ├── navigation/
│   │   └── NavigationModule.kt    # navigationEntryProvider bindings
│   └── presentation/
│       ├── PresentationModule.kt  # presenter bindings
│       └── ThemeModule.kt         # theme preference binding
├── domain/
│   ├── common/
│   │   └── Identifiable.kt        # interface { val id: Long }
│   ├── <entity>/                  # one per entity: model/, usecase/, repository interface
│   ├── search/                    # ISearchRepository, SearchUseCase, SearchResult
│   ├── sync/                      # SyncEngine, SyncApi, SyncChangeTracker, SyncEntityHandler + per-entity *SyncHandler
│   ├── export/                    # CsvExporter, ExportCsvUseCase, PdfGenerator (expect), ExportPatientReportUseCase
│   ├── backup/                    # BackupPayload, BackupProvider, ExportBackupUseCase, RestoreBackupUseCase
│   ├── notification/              # NotificationScheduler (expect), NotificationPermission (expect)
│   ├── reminder/                  # ReminderScheduler
│   └── timeline/                  # timeline feed models/use cases
├── presentation/
│   ├── AnimallyApp.kt            # Root @Composable: AnimallyTheme + AnimallyNavHost
│   ├── navigation/
│   │   ├── Route.kt               # sealed interface Route : NavKey
│   │   ├── AnimallyNavigator.kt   # @Single: manages backStack StateList
│   │   ├── AnimallyNavigationViewModel.kt  # Base ViewModel with nav methods
│   │   └── AnimallyNavHost.kt     # @Composable: NavDisplay with koin entries
│   ├── theme/                     # AnimallyTheme, ThemeMode, Color, Type, DynamicColor
│   ├── common/                    # shared UI: glass/, state/, addEdit/, attachment/, layout/
│   ├── <feature>/                 # per-feature: <Feature>ViewModel.kt + view/<Feature>Screen.kt
│   │   (patientList, patientDetail, patientEdit, ownerList, ownerEdit, ownerDetail,
│   │    settings, search, timeline, coggins, reminder, customreminder, anamnese,
│   │    consultation, vaccination, weight, deworming, dentistry, lameness, surgery,
│   │    medication, labresult, imaging, farrier, reproduction, ultrasound, gestation,
│   │    repromedication, substance)

shared/src/commonMain/sqldelight/.../
└── data/
    ├── <entity>/<Entity>.sq       # one per entity (CRUD + FTS upsert)
    ├── search/SearchFts.sq        # FTS5 virtual table for global search
    ├── sync/SyncMetadata.sq       # sync change-tracking table
    └── migrations/                # 1.sqm initial schema through 5.sqm
```

## Key File Locations

**Entry Points:**
- `androidApp/src/main/kotlin/.../MainActivity.kt`: Android Activity — renders `AnimallyApp`
- `iosApp/iosApp/iOSApp.swift`: iOS SwiftUI entry
- `shared/src/commonMain/kotlin/.../presentation/AnimallyApp.kt`: Shared root composable

**Configuration:**
- `build.gradle.kts`: Root build config (KMP, Compose, detekt, ktlint, Git hooks)
- `settings.gradle.kts`: Module includes (`:androidApp`, `:shared`)
- `gradle.properties`: KMP and Android SDK properties
- `config/detekt/detekt.yml`: Detekt code quality rules

**Core Logic:**
- `shared/src/commonMain/kotlin/.../di/infra/AppModule.kt`: Koin component scan root
- `shared/src/commonMain/kotlin/.../di/database/AnimallyDatabaseFactory.kt`: Database creation with all adapters
- `shared/src/commonMain/kotlin/.../di/database/QueriesModule.kt`: All 20 entity query class bindings
- `shared/src/commonMain/kotlin/.../di/http/HttpClientModule.kt`: Ktor HTTP client for cloud sync
- `shared/src/commonMain/kotlin/.../data/sync/SyncEngineImpl.kt`: Sync engine implementation
- `shared/src/commonMain/kotlin/.../domain/sync/SyncEntityHandlerRegistry.kt`: Per-entity sync handler routing
- `shared/src/commonMain/kotlin/.../presentation/navigation/AnimallyNavigator.kt`: Back-stack navigation singleton

**Domain Models:**
- `shared/src/commonMain/kotlin/.../domain/owner/model/Owner.kt`: Owner model
- `shared/src/commonMain/kotlin/.../domain/patient/model/Patient.kt`: Patient model
- `shared/src/commonMain/kotlin/.../domain/common/Identifiable.kt`: Base interface

**Database Schema:**
- `shared/src/commonMain/sqldelight/.../data/migrations/1.sqm`: Initial migration (all tables)
- `shared/src/commonMain/sqldelight/.../data/migrations/5.sqm`: Latest incremental migration
- `shared/src/commonMain/sqldelight/.../data/<entity>/<Entity>.sq`: Per-entity CRUD queries
- `shared/src/commonMain/sqldelight/.../data/search/SearchFts.sq`: FTS5 global-search table
- `shared/src/commonMain/sqldelight/.../data/sync/SyncMetadata.sq`: Sync change-tracking table

**Adapters:**
- `shared/src/commonMain/kotlin/.../data/adapters/InstantAdapter.kt`: Instant ↔ Long column adapter
- `shared/src/commonMain/kotlin/.../data/adapters/LocalDateAdapter.kt`: LocalDate ↔ String column adapter

**Platform actuals:**
- `shared/src/androidMain/kotlin/.../di/infra/Initialize.android.kt`: Android `initKoin`
- `shared/src/iosMain/kotlin/.../di/infra/Initialize.ios.kt`: iOS `initKoin`
- `shared/src/androidMain/kotlin/.../di/AndroidDatabaseModule.kt`: Android `AndroidSqliteDriver`
- `shared/src/iosMain/kotlin/.../di/IosDatabaseModule.kt`: iOS `NativeSqliteDriver`
- `shared/src/androidMain/kotlin/.../di/dispatchers/Dispatchers.android.kt`: Android coroutine dispatchers
- `shared/src/iosMain/kotlin/.../di/dispatchers/Dispatchers.ios.kt`: iOS coroutine dispatchers (`Dispatchers.Default` for IO)

## Naming Conventions

**Files:** PascalCase matching class/object name: `OwnerListScreen.kt`, `AnimallyDatabaseFactory.kt`, `InstantAdapter.kt`

**Directories:** Lowercase kebab-case for feature/entity groupings: `ownerList/`, `patientList/`, `anamnese/`, `consultation/`

**Kotlin source dirs:** Package name mirrors path:
- Domain: `domain.<entity>.model/`, `domain.<entity>.usecase/`
- Data: `data.<entity>/`, `data.adapters/`
- Presentation: `presentation.<featureName>/`, `presentation.<featureName>.view/`
- DI: `di.database/`, `di.dispatchers/`, `di.infra/`, `di.navigation/`

**SQLDelight dirs:** Mirrors data layer package: `data/<entity>/<Entity>.sq`

**Modules:** Lowercase: `androidApp`, `shared`

## Where to Add New Code

**New entity (e.g., LabResult):**
1. `shared/src/commonMain/sqldelight/.../data/labresult/LabResult.sq` — SQL CRUD queries (include FTS5 upsert)
2. `shared/src/commonMain/kotlin/.../data/labresult/LabResultRepositoryImpl.kt` + `data/labresult/mapper/` — `@Single` impl + DTO→model mappers
3. `shared/src/commonMain/kotlin/.../domain/labresult/model/LabResult.kt` — `@Serializable` data class
4. `shared/src/commonMain/kotlin/.../domain/labresult/ILabResultRepository.kt` — repository interface
5. `shared/src/commonMain/kotlin/.../domain/labresult/usecase/` — use cases
6. `shared/src/commonMain/kotlin/.../presentation/labresult/` — ViewModel + screen composables
7. Register in `QueriesModule.kt` (queries binding) + `PresentationModule.kt` if explicit
8. Add adapter entry in `AnimallyDatabaseFactory.create()`
9. Add `LabResultSyncHandler` in `domain/sync/handlers/` and register in `SyncEntityHandlerRegistry` if the entity syncs

**New screen:**
1. `shared/src/commonMain/kotlin/.../presentation/<featureName>/<FeatureName>ViewModel.kt` — `@KoinViewModel`
2. `shared/src/commonMain/kotlin/.../presentation/<featureName>/view/<FeatureName>Screen.kt` — `@Composable`
3. Add route variant in `Route.kt`
4. Register entry in `NavigationModule.kt` (`navigation<Route.X> { ... }`)
5. Navigate via `animallyNavigator.navigateTo(Route.X(...))`

**New platform actual:**
1. Declare `expect` in `commonMain` under appropriate package
2. Provide `actual` in `androidMain` + `iosMain` (and `nativeMain` if shared Apple impl)

**New DI binding:**
1. Use `@Single`/`@Factory`/`@KoinViewModel` annotation on impl class for auto-wiring
2. Add explicit binding in dedicated `@Module` class if interface→impl binding needed
3. Register explicit module in platform `initKoin()` if platform-specific

**New test:**
- `shared/src/commonTest/kotlin/.../` — Multiplatform unit tests
- `shared/src/androidHostTest/kotlin/.../` — Android JVM-only tests (e.g., SQLDelight with `JdbcSqliteDriver`)
- `shared/src/iosTest/kotlin/.../` — iOS simulator tests

**Shared UI resources:**
- `shared/src/commonMain/composeResources/drawable/` — Compose Multiplatform drawable resources

**Code quality configuration:**
- `config/detekt/detekt.yml` — Centralized detekt rules
- `config/detekt/baseline/<module>.xml` — Per-module detekt baseline (suppress existing violations)
