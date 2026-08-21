# Architecture

## Pattern Overview

**Overall:** Clean Architecture with MVVM presentation layer, offline-first local persistence via SQLDelight, Koin DI, and Compose Multiplatform UI.

**Key Characteristics:**
- Three-layer architecture: `domain` (business logic + models), `data` (persistence + repositories), `presentation` (Compose UI + ViewModels)
- Unidirectional data flow: Screen → ViewModel → UseCase → Repository → SQLDelight
- Offline-first: SQLDelight is primary storage. Local operations never require network; cloud sync (Ktor HTTP) is a secondary layer on top
- Hybrid Koin DI: `@Single`/`@KoinViewModel` annotations on impls + explicit modules for platform-specific singletons and queries
- Navigation 3 with type-safe sealed interface `Route` hierarchy
- `expect`/`actual` for platform differences (DB driver, coroutine dispatchers, file storage)
- Soft delete (`isActive` flag) on all entities — no hard deletes
- App-layer foreign keys: no SQLite FK constraints; validation in use case layer

## Layers

**Domain Layer:**
- Purpose: Business logic, domain models, and use case contracts
- Location: `shared/src/commonMain/kotlin/.../domain/`
- Contains: Model data classes, `Identifiable` interface, per-entity repository interfaces and use case classes (one `domain/<entity>/` module per entity: `owner/`, `patient/`, `anamnese/`, `consultation/`, ...), plus cross-cutting modules: `search/` (FTS5 search), `sync/` (sync contracts + per-entity sync handlers), `export/` (CSV/PDF), `backup/`, `notification/`, `reminder/`, `timeline/`
- Depends on: Kotlin stdlib, `kotlinx.serialization`, `kotlinx.datetime`
- Used by: Presentation layer (ViewModels call use cases)

**Data Layer:**
- Purpose: Persistence, DB queries, repository implementations, column adapters, sync engine, file storage
- Location: `shared/src/commonMain/kotlin/.../data/`
- Contains: SQLDelight `.sq` query definitions (20 entities + `SearchFts` + `SyncMetadata`), generated `Queries` classes, per-entity `*RepositoryImpl` with `mapper/` DTO→model mappers, `InstantAdapter`/`LocalDateAdapter` for column type mapping, `AnimallyDatabaseFactory`, `search/SearchRepository`, `storage/FileStorage` + `BackupStorage`, `backup/` restore impls, `sync/` (`KtorSyncApi`, `SyncEngineImpl`, `SyncChangeTrackerImpl`)
- Depends on: SQLDelight generated code (`AnimallyDatabase`), domain layer interfaces, Ktor HTTP client (sync only)
- Used by: DI (`QueriesModule` provides individual `*Queries` singletons), domain layer

**Presentation Layer:**
- Purpose: Compose UI screens, ViewModels, navigation
- Location: `shared/src/commonMain/kotlin/.../presentation/`
- Contains: `AnimallyApp` (root `@Composable`), screen composables and ViewModels per feature (`patientList/`, `patientDetail/`, `ownerList/`, `settings/`, `search/`, `timeline/`, plus per-entity `patientEdit/`, `consultation/`, `vaccination/`, ...), `common/` shared UI (`glass/`, `state/`, `addEdit/`, `attachment/`, `layout/`), theme (`theme/` with `ThemeMode`), navigation (`Route` sealed interface, `AnimallyNavigator`, `AnimallyNavHost`, `AnimallyNavigationViewModel`)
- Depends on: Domain layer (use cases + models)
- Used by: Platform entry points (Android `MainActivity`, iOS `MainViewController`)

**DI / Infrastructure Layer:**
- Purpose: Koin module configuration, platform initialization
- Location: `shared/src/*Main/kotlin/.../di/`
- Contains: `AppModule` (component scan), `QueriesModule` (query bindings), `DispatchersModule` (named coroutine dispatchers), `HttpClientModule` (Ktor client for sync), `PresentationModule` (presenter bindings), `ThemeModule`, `NavigationModule` (entry provider bindings), platform database modules (`AndroidDatabaseModule`, `IosDatabaseModule`), `Initialize.kt` (`expect fun initKoin`)
- Depends on: All other layers (provides wiring)
- Used by: Platform entry points call `initKoin()` on startup

**Platform Layer (expect/actual):**
- Purpose: Platform-specific implementations
- Location: `shared/src/androidMain/`, `shared/src/iosMain/`
- Contains: `SqlDriver` actual (Android: `AndroidSqliteDriver`, iOS: `NativeSqliteDriver`), coroutine dispatcher actuals (`Dispatchers.IO` vs `Dispatchers.Default`), `getPlatform()` actuals, platform file storage
- Depends on: Platform SDKs

## Data Flow

**App Initialization:**
1. Android: `AnimallyApplication.onCreate()` or iOS: `MainViewController()` → `initKoin(context)`
2. `initKoin` starts Koin with `AppModule` (component scan), platform `DatabaseModule`, `QueriesModule`, `HttpClientModule`, `PresentationModule`, `ThemeModule`, `navigationEntryModule`
3. `AnimallyApp` composable renders → `AnimallyNavHost` renders current route
4. `AnimallyNavigator` (singleton) manages `backStack: SnapshotStateList<Route>` — starts at `Route.PatientList`

**Navigation Flow:**
1. Screen ViewModel calls `navigateTo(route)` (inherited from `AnimallyNavigationViewModel`)
2. Delegates to `AnimallyNavigator.navigateTo()` — appends to `backStack`
3. `AnimallyNavHost` observes `backStack` via `NavDisplay` — renders composable for current route
4. Pop back: `ViewModel.popBackStack()` → `AnimallyNavigator.popBackStack()` → removes last element

**Database Interaction Flow:**
1. `AnimallyDatabaseFactory.create(driver)` instantiates `AnimallyDatabase` with all column adapters (`InstantAdapter` for epoch-millis ↔ `Instant`, `LocalDateAdapter` for ISO string ↔ `LocalDate`)
2. Each entity has a generated `*Queries` class (e.g., `PatientQueries`, `VaccinationQueries`)
3. `QueriesModule` binds each queries class as a Koin singleton
4. Repository (e.g., `OwnerRepositoryImpl`) receives queries via constructor injection
5. Use case calls repository; ViewModel calls use case

**Cloud Sync Flow:**
1. `SyncEngineImpl` drives push/pull via `SyncUseCase`
2. `SyncChangeTrackerImpl` reads `SyncMetadata` (serverId + dirty flags) to compute changed records
3. `SyncEntityHandlerRegistry` routes each changed entity to its per-entity `*SyncHandler`
4. `KtorSyncApi` (wired by `HttpClientModule`) POSTs `SyncPushRequest` and fetches `SyncPullResponse`
5. Handlers apply pulled records locally, resolving `serverId` mapping; failures return `SyncResult` surfaced in `SettingsScreen` Cloud Sync section

**Search Flow:**
1. `SearchUseCase` queries the `SearchFts` FTS5 virtual table via `SearchRepository`
2. Repositories upsert into FTS on entity write; results map to `SearchResult` and render in `SearchScreen`

**Screen Data Flow:**
1. ViewModel is created via `@KoinViewModel` annotation (Koin manages lifecycle)
2. ViewModel receives `AnimallyNavigator` and use cases via constructor injection
3. ViewModel exposes `StateFlow<UiState>` for screen state
4. Screen composable observes state, renders UI, calls ViewModel methods on user interaction
5. ViewModel invokes use case → repository → SQLDelight → updates state

## Key Abstractions

**`Identifiable`:**
- Purpose: Base interface for all domain models with an `id: Long` field
- Location: `shared/src/commonMain/kotlin/.../domain/common/Identifiable.kt`
- Pattern: Interface, implemented by data classes

**`AnimallyNavigator`:**
- Purpose: Singleton managing navigation back stack as `SnapshotStateList<Route>`
- Location: `shared/src/commonMain/kotlin/.../presentation/navigation/AnimallyNavigator.kt`
- Pattern: Single `@Single` with `mutableStateListOf(Route.PatientList)` as initial stack

**`AnimallyNavigationViewModel`:**
- Purpose: Base class for ViewModels that need navigation. Delegates `navigateTo`/`popBackStack` to `AnimallyNavigator`
- Location: `shared/src/commonMain/kotlin/.../presentation/navigation/AnimallyNavigationViewModel.kt`
- Pattern: Abstract `ViewModel` subclass

**`Route` (sealed interface):**
- Purpose: Type-safe navigation destinations. `@Serializable` sealed interface extending `NavKey`
- Location: `shared/src/commonMain/kotlin/.../presentation/navigation/Route.kt`
- Pattern: Sealed interface with `data object` (no-arg) and `data class` (parameterized) variants

**`AnimallyDatabaseFactory`:**
- Purpose: Factory creating `AnimallyDatabase` with all `ColumnAdapter` instances for `Instant` and `LocalDate`
- Location: `shared/src/commonMain/kotlin/.../di/database/AnimallyDatabaseFactory.kt`
- Pattern: Singleton `object` with `create(driver)` method

**Column Adapters (`InstantAdapter`, `LocalDateAdapter`):**
- Purpose: Convert between SQLite storage types and Kotlin types (`Instant` ↔ `Long`, `LocalDate` ↔ `String`)
- Location: `shared/src/commonMain/kotlin/.../data/adapters/`
- Pattern: `object` implementing `ColumnAdapter<T, TColumn>`

**Queries Modules (20 entity `.sq` files + `SearchFts` + `SyncMetadata`):**
- Purpose: Define SQL CRUD operations for each entity. Generated into type-safe `*Queries` classes
- Location: `shared/src/commonMain/sqldelight/.../data/<entity>/<Entity>.sq`
- Pattern: Each `.sq` file defines `insert`, `update`, `delete`, `selectById`, `selectByPatient`, `selectAll`, `setInactive` queries. `QueriesModule` binds each generated class as a Koin singleton

## Entry Points

**Android:**
- Location: `androidApp/src/main/kotlin/.../MainActivity.kt`
- Triggers: System launches Activity
- Responsibilities: `setContent { AnimallyApp() }` — renders Compose UI
- Init: `AnimallyApplication.onCreate()` calls `initKoin(this)` before any activity

**iOS:**
- Location: `iosApp/iosApp/iOSApp.swift` → `ContentView.swift` → `MainViewControllerKt.MainViewController()`
- Triggers: System launches app
- Responsibilities: Creates `ComposeUIViewController` wrapping `AnimallyApp()`; Phase 7 adds a native SwiftUI shell (`Patients/`, `Search/`, `Settings/`, `Theme/`, `Timeline/`) backed by exported Kotlin stores (`bridge/IosRecordStores`, `IosReproAndDiagnosticsStores`, `IosSettingsStores` in `iosMain`)
- Init: `MainViewController()` calls `initKoin()` before `AnimallyApp()`

**Shared:**
- Location: `shared/src/commonMain/kotlin/.../presentation/AnimallyApp.kt`
- Triggers: Called by platform entry points
- Responsibilities: Root `@Composable` — wraps `AnimallyNavHost` in `AnimallyTheme` (theme mode read from `SettingsViewModel.themeMode`)
- Init: Performed before this composable renders (in platform `initKoin`)

## Database Schema (20 Entities + FTS5 + SyncMetadata)

All entities follow this pattern:
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `patientId INTEGER NOT NULL` (app-layer FK, except `Owner`, `Patient`)
- `isActive INTEGER NOT NULL DEFAULT 1` (soft delete)
- `createdAt INTEGER AS Instant NOT NULL`
- `updatedAt INTEGER AS Instant NOT NULL`
- `date`/`dateAdministered` columns use `TEXT AS LocalDate`
- Migrations: `migrations/1.sqm` initial schema through `5.sqm` (incremental)

**Entities:** Anamnese, Consultation, CustomReminder, Dentistry, Deworming, FarrierVisit, Gestation, Imaging, LabResult, Lameness, Medication, Owner, Patient, Reproduction, ReproMedication, Substance, Surgery, Ultrasound, Vaccination, Weight

**Support tables:**
- `SearchFts` (FTS5 virtual table) for global search, populated on entity writes
- `SyncMetadata` (serverId + sync state) for cloud sync change tracking

## Error Handling

**Strategy:** Fail-open during Phase 1-2 (early development). SQLDelight throws on constraint violation. App-layer FK enforcement in use cases (not SQLite foreign keys). Soft delete prevents accidental data loss. Migration failure triggers fallback to backup (ADR-0022).

## Cross-Cutting Concerns

**Logging:** Not yet implemented. Detekt + ktlint for static analysis. Pre-commit hook installed via `installGitHooks` Gradle task.

**Caching:** Not needed — SQLite is primary storage (offline-first). No remote cache layer.

**Storage:** SQLite via SQLDelight. File storage (`data/storage/FileStorage`, `BackupStorage`) via `expect`/`actual` platform filesystem — `Context.filesDir` on Android, `NSDocumentDirectory` on iOS. Backup/restore (JSON payloads) and PDF export implemented with platform actuals.

**Notifications:** `expect`/`actual` `NotificationScheduler` + `NotificationPermission`; Android uses notification channels, iOS uses its own scheduler. Reminders scheduled via `ReminderScheduler`.

**Testing:** Five test source sets: `commonTest` (multiplatform unit tests), `androidHostTest` (Android JVM-only tests), `iosTest` (iOS simulator tests), `desktopTest` (headless Compose UI tests), plus Kover coverage tooling. In-memory SQLDelight driver for test speed. Mock test doubles via interface implementations.

**Code Quality:** Detekt (custom config at `config/detekt/detekt.yml`), ktlint (via Gradle plugin), baseline files per module at `config/detekt/baseline/`.
