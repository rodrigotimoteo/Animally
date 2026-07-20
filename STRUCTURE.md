# Codebase Structure

## Directory Layout

```
Animally/
├── androidApp/              # Android application module
│   └── src/main/kotlin/...  # MainActivity, AnimallyApplication
├── iosApp/                  # iOS application module
│   └── iosApp/              # iOSApp.swift, ContentView.swift
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
│       └── iosTest/         # iOS simulator tests
├── config/detekt/           # Detekt static analysis config
├── docs/adr/                # Architecture Decision Records
├── build.gradle.kts         # Root build config + detekt plugin
└── settings.gradle.kts      # Module includes
```

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
  - `androidMain`: `actual` implementations (Android `SqlDriver`, coroutine dispatchers, platform info)
  - `iosMain`: `actual` implementations (iOS `NativeSqliteDriver`, coroutine dispatchers, platform info)
  - `nativeMain`: Shared Apple-target `actual` implementations (empty currently)
  - `commonTest`: Multiplatform unit tests (placeholder)
  - `androidHostTest`: Android host JVM tests (placeholder)
  - `iosTest`: iOS simulator tests (placeholder)

## Shared Module Internal Layout

```
shared/src/commonMain/kotlin/.../
├── Platform.kt                    # expect fun getPlatform()
├── data/
│   ├── adapters/                  # ColumnAdapter for Instant, LocalDate
│   │   ├── InstantAdapter.kt
│   │   └── LocalDateAdapter.kt
│   ├── owner/
│   │   └── OwnerRepositoryImpl.kt  # @Single IOwnerRepository impl
│   ├── patient/                    # (empty — PatientRepository not yet created)
│   └── anamnese/ consultation/ ... # (future repository impls)
├── di/
│   ├── database/
│   │   ├── AnimallyDatabaseFactory.kt  # Factory creating DB with adapters
│   │   └── QueriesModule.kt            # Binds all *Queries singletons
│   ├── dispatchers/
│   │   ├── Dispatchers.kt         # expect declarations (io, main, default)
│   │   └── DispatchersModule.kt   # @Module providing named dispatchers
│   ├── infra/
│   │   ├── AppModule.kt           # @Module @ComponentScan for DI
│   │   └── Initialize.kt          # expect fun initKoin()
│   └── navigation/
│       └── NavigationModule.kt    # navigationEntryProvider bindings
├── domain/
│   ├── common/
│   │   └── Identifiable.kt        # interface { val id: Long }
│   ├── owner/
│   │   ├── IOwnerRepository.kt    # Repository interface
│   │   ├── model/
│   │   │   └── Owner.kt           # @Serializable data class
│   │   └── usecase/
│   │       ├── GetOwnerListUseCase.kt    # @Single (stub)
│   │       └── GetOwnerDetailUseCase.kt  # @Single (stub)
│   ├── patient/
│   │   ├── model/
│   │   │   └── Patient.kt         # data class (id only, placeholder)
│   │   └── usecase/               # (empty)
│   ├── anamnese/ consultation/ ... # (future domain modules — see note)
├── presentation/
│   ├── AnimallyApp.kt            # Root @Composable: Theme + AnimallyNavHost
│   ├── navigation/
│   │   ├── Route.kt               # sealed interface Route : NavKey
│   │   ├── AnimallyNavigator.kt   # @Single: manages backStack StateList
│   │   ├── AnimallyNavigationViewModel.kt  # Base ViewModel with nav methods
│   │   └── AnimallyNavHost.kt     # @Composable: NavDisplay with koin entries
│   ├── ownerList/
│   │   ├── OwnerListViewModel.kt  # @KoinViewModel (stub)
│   │   └── view/
│   │       └── OwnerListScreen.kt # @Composable (stub)
│   ├── patientList/
│   │   ├── PatientListViewModel.kt  # @KoinViewModel (stub)
│   │   └── view/
│   │       └── PatientListScreen.kt # @Composable (stub)
│   └── settings/
│       ├── SettingsViewModel.kt    # @KoinViewModel (stub)
│       └── view/
│           └── SettingsScreen.kt   # @Composable (stub)

shared/src/commonMain/sqldelight/.../
└── data/
    ├── anamnese/Anamnese.sq
    ├── consultation/Consultation.sq
    ├── dentistry/Dentistry.sq
    ├── deworming/Deworming.sq
    ├── farrier/FarrierVisit.sq
    ├── gestation/Gestation.sq
    ├── imaging/Imaging.sq
    ├── labresult/LabResult.sq
    ├── lameness/Lameness.sq
    ├── medication/Medication.sq
    ├── migrations/1.sqm          # Initial schema migration
    ├── owner/Owner.sq
    ├── patient/Patient.sq
    ├── reproduction/Reproduction.sq
    ├── repromedication/ReproMedication.sq
    ├── substance/Substance.sq
    ├── surgery/Surgery.sq
    ├── ultrasound/Ultrasound.sq
    ├── vaccination/Vaccination.sq
    └── weight/Weight.sq
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
- `shared/src/commonMain/kotlin/.../di/database/QueriesModule.kt`: All 19 query class bindings
- `shared/src/commonMain/kotlin/.../presentation/navigation/AnimallyNavigator.kt`: Back-stack navigation singleton

**Domain Models:**
- `shared/src/commonMain/kotlin/.../domain/owner/model/Owner.kt`: Owner model (most complete)
- `shared/src/commonMain/kotlin/.../domain/patient/model/Patient.kt`: Patient model (stub)
- `shared/src/commonMain/kotlin/.../domain/common/Identifiable.kt`: Base interface

**Database Schema:**
- `shared/src/commonMain/sqldelight/.../data/migrations/1.sqm`: Initial migration (all 19 tables)
- `shared/src/commonMain/sqldelight/.../data/<entity>/<Entity>.sq`: Per-entity CRUD queries

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
1. `shared/src/commonMain/sqldelight/.../data/labresult/LabResult.sq` — SQL CRUD queries
2. `shared/src/commonMain/kotlin/.../data/labresult/LabResultRepositoryImpl.kt` — `@Single` impl
3. `shared/src/commonMain/kotlin/.../domain/labresult/model/LabResult.kt` — `@Serializable` data class
4. `shared/src/commonMain/kotlin/.../domain/labresult/ILabResultRepository.kt` — repository interface
5. `shared/src/commonMain/kotlin/.../domain/labresult/usecase/` — use cases
6. `shared/src/commonMain/kotlin/.../presentation/labresult/` — ViewModel + screen composables
7. Register in `QueriesModule.kt` (queries binding)
8. Add adapter entry in `AnimallyDatabaseFactory.create()`

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
