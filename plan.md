# Animally — Implementation Plan

Generated from Grill Me session. 12 rounds, 33 decisions, 22 ADRs.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture Decisions](#architecture-decisions)
3. [Data Model](#data-model)
4. [Storage Strategy](#storage-strategy)
5. [Navigation Structure](#navigation-structure)
6. [Build Configuration](#build-configuration)
7. [DI & Concurrency](#di--concurrency)
8. [Search Architecture](#search-architecture)
9. [Reminders & Notifications](#reminders--notifications)
10. [Export & Backup](#export--backup)
11. [Testing Strategy](#testing-strategy)
12. [Failure Modes & Data Integrity](#failure-modes--data-integrity)
13. [Phase Split](#phase-split)
14. [Non-Goals](#non-goals)
15. [ADR Index](#adr-index)

---

## 1. Project Overview

**Animally** — KMP vet clinic app for equine veterinarians. Single vet, single phone. Offline-first. Phase 6 adds cloud sync.

**Core workflow:** Vet opens app → selects horse → records consultation (SOAP notes) → tracks vaccinations, deworming, dentistry → manages reproduction cycle → searches across all records.

**Key constraint:** No network code in Phase 1-3. All operations work offline. Cloud sync is secondary, deferred.

---

## 2. Architecture Decisions

### 2.1 UI Framework — Compose Multiplatform

**Decision:** Compose Multiplatform (not SwiftUI native, not split codebases).

**Why:** Single codebase across Android/iOS. Shared UI logic. KMP ecosystem alignment.

**Alternatives rejected:**
- SwiftUI native: Would require separate UI codebases. No code sharing benefit from KMP.
- Split: Compose Android + SwiftUI iOS. Maximum platform fidelity but doubles UI work.

**Consequence:** iOS UI may feel slightly non-native. Acceptable for utility app. Can add platform-specific polish later.

### 2.2 Navigation — Navigation 3

**Decision:** Navigation 3 (not Voyager, not Decompose).

**Why:** Type-safe routes. KMP support. Google-backed, stable API.

**Alternatives rejected:**
- Voyager: Community-maintained, less stable API, fewer KMP examples.
- Decompose: More powerful (lifecycle, back stack manipulation) but overkill for this app's navigation depth.

**Consequence:** Navigation 3 is newer, smaller ecosystem. But routes are compile-time safe.

### 2.3 Database — SQLDelight

**Decision:** SQLDelight 2.3.2 (not Room, not Realm).

**Why:** Only KMP-compatible local DB with compile-time query validation. `.sq` files produce type-safe Kotlin APIs.

**Alternatives rejected:**
- Room: Android-only. No KMP support. Would require separate iOS DB layer.
- Realm: MongoDB-backed, larger binary, less control over schema.

**Consequence:** `.sqm` migration files required from day 1 (ADR-0018). More manual schema management than Room's auto-migration. But full control.

### 2.4 DI — Koin

**Decision:** Koin 4.2.2 with K2 compiler plugin + Koin Annotations.

**Why:** Only KMP-compatible DI. K2 compiler validates graph at compile time (catches missing bindings before runtime).

**Alternatives rejected:**
- Hilt: Android-only. No KMP support.
- Manual DI: More control but massive boilerplate for 19 entities × (repo + use case + VM).

**Consequence:** Hybrid module structure (ADR-0016) — annotations on impls, explicit modules for interface→impl bindings.

---

## 3. Data Model

### 3.1 Contradictions Found

`idea.md` and `data.md` disagreed on multiple entities. Key contradictions:

| Entity | idea.md | data.md | Resolution |
|--------|---------|---------|------------|
| Patient | Had `current_weight`, `stable_location`, `coggins_*`, `ueln`, `age`, `sex` | Had `dateOfBirth`, `gender`, `photoUri`, `registrationNumber`, `isActive` — no weight/stable/coggins/ueln | Merge: `data.md` structure + re-add missing fields |
| Anamnese | Separate entity: `general_history`, `chronic_conditions`, `allergies` | Missing entirely | Re-add as separate entity |
| Weight | "tracked as history, not single value" | No Weight entity | Re-add as `WeightEntry` |
| DeWorming | Listed in Phase 1 | No entity — merged into Vaccination | Separate entity |
| Ultrasound | `ovary_status`, `uterine_status`, `follicle_size` (repro-specific) | `findings`, `imageUris` (generic) | Add repro fields |
| Gestation | `insemination_date`, `gestation_days` | `breedingDate`, `status`, `fetalCount`, `lastCheckDate` | Add `gestationDays` |
| MedicationLog | `dose: float`, `unit: string` | `dosage: String`, `route`, `frequency`, `startDate`, `endDate` | `data.md` shape wins |

### 3.2 Final Entity List (19 entities)

#### Phase 1a — Core (5 entities)

**Patient**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
name: TEXT NOT NULL
species: TEXT NOT NULL (default "Equine")
breed: TEXT
dateOfBirth: TEXT (ISO LocalDate)
gender: TEXT (Male/Female/Other)
microchipId: TEXT
ueln: TEXT (15-digit ISO, microchip-linked)
registrationNumber: TEXT (studbook/federation)
stableLocation: TEXT
photoUri: TEXT
notes: TEXT
ownerIds: TEXT (comma-separated, app-layer FK)
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL (epoch millis)
updatedAt: INTEGER NOT NULL (epoch millis)
```

**Why separate UELN + registrationNumber:** Different identifiers. UELN is 15-digit ISO microchip-linked. RegistrationNumber is studbook/federation. Both needed for equine identification.

**Why stableLocation on Patient:** Horses move between stables. Single field, not history. If history needed, can add later.

**Why Coggins on Patient:** Coggins test result + expiry is a single point-in-time check, not history. Expiry triggers reminder. If history needed, can extract to separate entity later.

**Owner**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
name: TEXT NOT NULL
phone: TEXT
email: TEXT
address: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Anamnese** (1:1 with Patient)
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER UNIQUE NOT NULL (app-layer FK)
generalHistory: TEXT
chronicConditions: TEXT
allergies: TEXT
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why separate entity:** Long text fields. Bloating Patient row with 3 text columns hurts scan performance. 1:1 relationship allows lazy loading — only fetch when user opens Anamnese tab.

**Consultation**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL (ISO LocalDate)
subjective: TEXT (owner's description)
objective: TEXT (exam findings)
assessment: TEXT (diagnosis)
plan: TEXT (treatment)
vetName: TEXT
nextVisitDate: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Vaccination**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
vaccineName: TEXT NOT NULL
dateAdministered: TEXT NOT NULL (ISO LocalDate)
nextDueDate: TEXT (computed by use case on save)
veterinarian: TEXT
batchNumber: TEXT
site: TEXT (injection site)
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why nextDueDate computed at save:** Use case layer calculates from vaccine type + date administered. Stored in DB for fast "what's due next week" queries. Recalculated on edit.

#### Phase 1b — Remaining (14 entities)

**WeightEntry**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
weightKg: REAL NOT NULL
date: TEXT NOT NULL (ISO LocalDate)
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
```

**Why separate entity:** History tracking. Single value on Patient loses trend data. Vet needs to see weight over time for dosage calculations.

**Deworming** (separate from Vaccination)
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
product: TEXT NOT NULL
dateAdministered: TEXT NOT NULL (ISO LocalDate)
nextDueDate: TEXT
dose: TEXT
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why separate from Vaccination:** Different drug class (anthelmintic vs vaccine). Different reminder cadence (monthly/quarterly vs annual/semi-annual). Different fields (no batchNumber/site, has dose). Merging with `type` field would add conditional logic everywhere.

**Dentistry**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL
findings: TEXT
treatment: TEXT
nextDueDate: TEXT (3/6/9/12 month schedule)
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Lameness**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL
gradeAAEP: INTEGER (1-5 scale)
limbLocation: TEXT
flexionTest: TEXT
diagnosis: TEXT
treatment: TEXT
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Surgery**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL
type: TEXT
description: TEXT
outcome: TEXT
surgeon: TEXT
anesthesia: TEXT
    analgesia: TEXT
    complications: TEXT
recoveryNotes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Medication**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
name: TEXT NOT NULL
dosage: TEXT NOT NULL
route: TEXT (oral/IV/IM/topical)
frequency: TEXT
startDate: TEXT
endDate: TEXT
prescribedBy: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**LabResult**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
testType: TEXT NOT NULL
date: TEXT NOT NULL
results: TEXT
normalRange: TEXT
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Imaging**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
type: TEXT (X-ray/Ultrasound/Other)
date: TEXT NOT NULL
findings: TEXT
imageUris: TEXT (comma-separated paths)
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**FarrierVisit**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL
trimOrShoe: TEXT
shoeType: TEXT
findings: TEXT
nextDueDate: TEXT
farrier: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**ReproductionEvent**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
eventType: TEXT NOT NULL (Heat/Breeding/PregnancyCheck/Foaling)
date: TEXT NOT NULL
details: TEXT
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Ultrasound** (repro-specific)
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
date: TEXT NOT NULL
ovaryStatus: TEXT
uterineStatus: TEXT
follicleSizeMm: REAL
findings: TEXT
imageUris: TEXT (comma-separated paths)
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why repro fields:** Structured data for follicle tracking (size in mm). `ovaryStatus` and `uterineStatus` are critical for breeding cycle management. Generic `findings` text loses queryable structure.

**Gestation**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
breedingDate: TEXT NOT NULL
expectedDueDate: TEXT NOT NULL (computed at save)
gestationDays: INTEGER NOT NULL (computed at save, recalculated on read)
status: TEXT (Active/Completed/Failed)
fetalCount: INTEGER
lastCheckDate: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why gestationDays computed:** Equine gestation ~340 days. `gestationDays` = days since `breedingDate`. Recalculated on read for real-time progress. Stored for fast queries ("show all horses at 300+ days").

**ReproMedication**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
medication: TEXT NOT NULL
dateAdministered: TEXT NOT NULL
dosage: TEXT
purpose: TEXT
veterinarian: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**ControlledSubstance**
```
id: INTEGER PRIMARY KEY AUTOINCREMENT
patientId: INTEGER NOT NULL (app-layer FK)
drugName: TEXT NOT NULL
dose: TEXT NOT NULL
unit: TEXT
route: TEXT
administeredBy: TEXT
witness: TEXT
date: TEXT NOT NULL
reason: TEXT
notes: TEXT
isActive: INTEGER NOT NULL (default 1)
createdAt: INTEGER NOT NULL
updatedAt: INTEGER NOT NULL
```

**Why standard CRUD (no audit log):** Personal app for now. If sharing with 3rd parties later, revisit append-only + audit log. Regulatory requirements vary by jurisdiction.

### 3.3 ID Strategy

**Decision:** Auto-increment INTEGER PRIMARY KEY.

**Why:** Simple, fast, no UUID generation overhead. DB assigns IDs.

**Why not UUID:** Defers sync complexity to Phase 6. UUIDs are larger indexes, string comparisons slower. If Phase 6 needs UUIDs, can migrate with `ALTER TABLE` + backfill.

**Consequence:** IDs are not portable across devices. Sync will require ID mapping table in Phase 6.

### 3.4 Deletion Strategy

**Decision:** App-layer FK enforcement. No DB cascade.

**Why:** SQLDelight doesn't enforce FKs by default. Enabling `PRAGMA foreign_keys = ON` + `ON DELETE CASCADE` is dangerous — deleting an Owner would cascade-delete all Patients and all records.

**Pattern:**
```kotlin
// Repository checks dependencies before delete
suspend fun deleteOwner(ownerId: Long) {
    val patients = patientRepository.getByOwnerId(ownerId)
    if (patients.isNotEmpty()) {
        throw IllegalStateException("Owner has ${patients.size} patients. Unassign first.")
    }
    ownerRepository.softDelete(ownerId)
}
```

**Soft delete:** `isActive` flag on Patient. Never hard-delete. Medical records must be preserved.

---

## 4. Storage Strategy

### 4.1 File Attachments

**Decision:** App-internal storage.

| Platform | Path |
|----------|------|
| Android | `Context.filesDir` |
| iOS | `NSDocumentDirectory` |

**Why internal:** No permissions needed. Safe from user deletion. Survives app updates.

**Drawback:** Lost on uninstall. Mitigated by backup/restore in Phase 3.

**Alternatives rejected:**
- Shared storage (user-visible): Android 11+ scoped storage restricts access. Requires `MANAGE_EXTERNAL_STORAGE` (Play Store rejects) or `SAF` (user picks folder). iOS has no equivalent persistent shared location.
- Auto-backup to platform cloud: 25MB limit on Android, no control, unreliable timing.

### 4.2 Backup Format

**Decision:** Dual format.

| Format | Use case |
|--------|----------|
| JSON export | Portable, schema-versioned, human-readable. Default backup. |
| Raw DB copy | Fast, complete. For power users / migration recovery. |

**Why both:** JSON is portable across schema versions (can re-import after migration). Raw DB is instant but breaks on schema changes.

### 4.3 Migration Failure

**Decision:** Fallback to backup.

**Flow:**
1. App update triggers schema migration
2. Migration fails → show error dialog
3. Offer "Restore from backup" (if backup exists)
4. Offer "Export raw data" (user can save before wipe)
5. If no backup and no export → data lost (worst case)

**Why not silent recreate:** Medical records are critical. Never silently wipe data.

---

## 5. Navigation Structure

### 5.1 Tabbed PatientDetail

**Decision:** 5-tab layout.

```
PatientDetail
├── Overview tab
│   ├── Profile
│   ├── Anamnese
│   └── Weight History
├── Medical tab
│   ├── Consultations
│   ├── Surgeries
│   ├── Lameness
│   ├── Medications
│   └── Controlled Substances
├── Preventive tab
│   ├── Vaccinations
│   ├── Deworming
│   ├── Dentistry
│   ├── Coggins
│   └── Farrier
├── Reproduction tab
│   ├── Events
│   ├── Ultrasound
│   ├── Gestation
│   └── Repro Meds
└── Diagnostics/Files tab
    ├── Lab Results
    ├── Imaging
    ├── Files
    └── Export
```

**Why tabs over flat list:** 18+ entity types. Flat list requires scrolling through 18+ items. Tabs group by domain, reduce cognitive load.

**Why tabs over grouped list:** Grouped list still requires scrolling within groups. Tabs provide instant access to any domain.

**Alternatives rejected:**
- Flat list: Too long with 18+ entities.
- Grouped list: Better than flat but still scroll-heavy.
- Nested navigation: Too many levels. Users get lost.

### 5.2 New Routes

| Route | Entity | Location |
|-------|--------|----------|
| `AddEditAnamnese` | Anamnese (1:1 with Patient) | Overview tab |
| `AddEditWeight` | WeightEntry history | Overview tab |
| `AddEditDeworming` | Deworming (separate entity) | Preventive tab |

**Why Anamnese has dedicated route:** Separate entity with long text fields. Inline editing on Profile tab would bloat the UI.

**Why Weight has dedicated route:** History entity. Needs list view + add button.

**Why Deworming has dedicated route:** Separate entity from Vaccination. Different fields, different cadence.

---

## 6. Build Configuration

### 6.1 Missing Dependencies (Add Now)

| Dependency | Version | Why needed |
|-----------|---------|------------|
| `koin-compose-navigation3` | 4.2.2 | Koin + Navigation3 ViewModel scoping. Without this, VMs can't be scoped to routes. |
| `sqldelight-coroutines` | 2.3.2 | `asFlow()` queries. Without this, repos can't return reactive Flows. |
| `sqldelight-primitive-adapter` | 2.3.2 | `Instant`/`LocalDate` column adapters. Without this, custom types can't be stored. |
| `kotlinx-coroutines-test` | 1.10.2 | Test dispatcher support. Without this, VM tests can't control coroutine timing. |
| `sqldelight-sqlite-driver` (JVM) | 2.3.2 | `androidHostTest` in-memory DB. Without this, repo tests can't run on JVM. |

**Why add all now:** Phase 1 needs Flow queries + custom type adapters + Koin-scoped VMs + tests. Without these, code won't compile. Deferred addition causes build breaks mid-implementation.

### 6.2 Build Time

**Decision:** Accept build time. Measure later.

**Why:** 19 entities × `.sq` files + KSP (Koin) + SQLDelight codegen + Compose compiler = slow builds. But configuration cache is enabled. Optimize only if >3min clean build.

---

## 7. DI & Concurrency

### 7.1 Koin Module Structure (Hybrid)

**Decision:** Annotations on impls + explicit modules for interface→impl bindings.

**Pattern:**
```kotlin
// Annotations on implementations
@Single
class PatientRepositoryImpl(
    private val queries: PatientQueries,
    private val ioDispatcher: CoroutineDispatcher,
) : PatientRepository { ... }

// Explicit module for interface binding
@Module
class RepositoryModule : KoinModule {
    fun provide() = module {
        single<PatientRepository> { get<PatientRepositoryImpl>() }
        single<OwnerRepository> { get<OwnerRepositoryImpl>() }
        // ... all repos
    }
}

// DatabaseModule (platform-specific, can't annotate)
@Module
class DatabaseModule : KoinModule {
    fun provide() = module {
        single<SqlDriver> { /* platform-specific */ }
        single<AnimallyDatabase> { AnimallyDatabase(get()) }
    }
}
```

**Why hybrid:** Annotations alone can't express interface→impl bindings cleanly. `DatabaseModule` is platform-specific (needs `expect`/`actual`). Explicit modules give control where needed, annotations reduce boilerplate elsewhere.

### 7.2 Coroutine Dispatchers

**Decision:** Explicit `Dispatchers.IO` (Android) / `Dispatchers.Default` (iOS).

**Pattern:**
```kotlin
// commonMain
expect val ioDispatcher: CoroutineDispatcher

// androidMain
actual val ioDispatcher = Dispatchers.IO

// iosMain
actual val ioDispatcher = Dispatchers.Default
```

**Why explicit:** Predictable, what you're used to. SQLDelight doesn't auto-dispatch — queries run on calling thread. Repos must wrap all DB calls in `withContext(ioDispatcher)`.

**Repository pattern:**
```kotlin
class PatientRepositoryImpl(
    private val queries: PatientQueries,
    private val ioDispatcher: CoroutineDispatcher,
) : PatientRepository {
    override fun getAll(): Flow<List<Patient>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)
}
```

**Why not let caller decide:** More flexible but error-prone. Forgetting dispatcher = Main thread DB access = crash.

---

## 8. Search Architecture

### 8.1 FTS5 Index Scope

**Decision:** Patient-centric FTS5.

**Single `search_fts` virtual table joining:**
- Patient name, breed, microchipId
- Consultation assessment, plan
- Medication name

**Why patient-centric:** One search query returns patient matches with highlighted context. User searches "lameness" → sees patients with lameness consultations. Simpler than per-entity FTS.

**Alternatives rejected:**
- Per-entity FTS: More precise filtering but more tables, more complex query orchestration.
- Patient + Consultation only: Missing medication search.

### 8.2 Search Results Display

**Decision:** Grouped results with expandable patient records.

**Flow:**
1. User types search query
2. Results grouped by Patient
3. Each Patient expandable to show matching records
4. Tap a record → navigate to PatientDetail scrolled to that record

**Why grouped:** Search across 19 entity types. Ungrouped results would be chaotic. Grouping by Patient provides context.

**Why expandable:** Shows all matches per patient without navigating away from search.

**Why scroll to source:** Direct navigation to the specific record. No extra steps finding it in tabs.

---

## 9. Reminders & Notifications

### 9.1 Next-Due Computation

**Decision:** Use case layer computes `nextDueDate` on save. Stored in DB.

**Pattern:**
```kotlin
class CalculateNextVaccinationUseCase {
    operator fun invoke(vaccineName: String, dateAdministered: LocalDate): LocalDate {
        val intervalMonths = when (vaccineName) {
            "Tetanus" -> 12
            "Influenza" -> 6
            "Rhinopneumonitis" -> 6
            // ... other vaccines
            else -> 12
        }
        return dateAdministered.plusMonths(intervalMonths)
    }
}
```

**Why computed at save:** Fast "what's due next week" queries. No runtime computation needed. Recalculated on edit if vaccine type changes.

**Why stored:** Ad-hoc recalculation via use case also supported. Redundant but flexible.

### 9.2 Notification Scheduling

**Decision:** Platform-native via `expect`/`actual`.

| Platform | API |
|----------|-----|
| Android | `AlarmManager` + `NotificationManager` |
| iOS | `UNUserNotificationCenter` |

**Why platform-native:** Full platform control. No third-party dependency risk. KMP-Notifier library maturity varies.

**Why not KMP-Notifier:** Single dependency risk. Platform-native gives full control over notification behavior, channels, categories.

**Phase:** Phase 4. Phase 1-3: store next-due, no notifications.

---

## 10. Export & Backup

### 10.1 PDF Generation

**Decision:** JVM PDF lib + PDFKit iOS via `expect`/`actual`.

| Platform | Library |
|----------|---------|
| Android | iText / OpenPDF |
| iOS | PDFKit |

**Why platform-specific:** JVM PDF libs don't work on iOS. PDFKit is native iOS. `expect`/`actual` provides unified API.

**Alternatives rejected:**
- KMP PDF library: Library risk. Few mature options.
- HTML-to-PDF: Less control over layout. WebView rendering varies.

**Phase:** Phase 3.

### 10.2 Backup Workflow

**Flow:**
1. User triggers backup (manual in Phase 3)
2. App serializes all entities to JSON + copies raw DB
3. User chooses save location (share sheet)
4. Backup file contains: `backup_YYYY-MM-DD.json` + `animally.db`
5. Restore: user picks backup file → app imports JSON + restores DB

**Why manual:** Cloud sync in Phase 6 handles automatic backup. Manual backup is for pre-update safety / data portability.

---

## 11. Testing Strategy

### 11.1 Test Targets

**Decision:** commonTest + iosTest + androidHostTest.

| Target | What's tested |
|--------|---------------|
| commonTest | All logic (repos, use cases, VMs) on JVM |
| androidHostTest | Android-specific code (file storage, notifications) |
| iosTest | iOS-specific code (file storage, notifications) |

**Why all three:** Full platform coverage. commonTest catches logic bugs. Platform tests catch platform-specific issues.

### 11.2 Test Doubles

| Layer | Strategy |
|-------|----------|
| Repository tests | In-memory SQLDelight DB (JVM `JdbcSqliteDriver` with `:memory:`) |
| Use case tests | Mocked repository interfaces |
| ViewModel tests | Turbine + in-memory repo (realistic) |

**Why in-memory DB for repos:** Real SQLite, real schema, real queries. Catches SQL syntax errors, type adapter bugs, migration issues.

**Why mock repos for use cases:** Use cases are business logic. Testing with mocked repos isolates the logic from DB implementation.

**Why Turbine + in-memory repo for VMs:** Tests VM + repo + DB together. Realistic flow testing. Turbine provides clean Flow assertion API.

### 11.3 In-Memory DB Setup

```kotlin
// commonTest
fun inMemoryDatabase(): AnimallyDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Schema.create(driver)
    return AnimallyDatabase(driver)
}
```

---

## 12. Failure Modes & Data Integrity

### 12.1 Foreign Key Enforcement

**Decision:** App-layer enforcement. Repos check dependencies before delete.

**Pattern:**
```kotlin
class PatientRepositoryImpl(...) : PatientRepository {
    override suspend fun delete(patientId: Long) {
        // Check all dependent entities
        val consultations = consultationQueries.getByPatient(patientId).executeAsList()
        val vaccinations = vaccinationQueries.getByPatient(patientId).executeAsList()
        val dewormings = dewormingQueries.getByPatient(patientId).executeAsList()
        // ... check all 19 entities

        if (consultations.isNotEmpty() || vaccinations.isNotEmpty() || dewormings.isNotEmpty()) {
            throw IllegalStateException("Patient has records. Delete records first or use soft delete.")
        }

        // Soft delete patient
        patientQueries.setInactive(patientId)
    }
}
```

**Why not DB cascade:** Accidental deletion of medical records is catastrophic. Explicit checks force developer to handle dependencies.

### 12.2 Migration Failure

**Decision:** Fallback to backup.

**Flow:**
1. App update triggers schema migration
2. Migration fails → show error dialog
3. Offer "Restore from backup" (if backup exists)
4. Offer "Export raw data" (user can save before wipe)
5. If no backup and no export → data lost (worst case)

**Why not crash + log:** User needs recovery path. Crash without recovery = data loss.

**Why not silent recreate:** Medical records are critical. Never silently wipe data.

### 12.3 Controlled Substance Audit

**Decision:** Standard CRUD for now.

**Why lenient:** Personal app. No regulatory requirement until sharing with 3rd parties.

**Future:** If sharing, add append-only + audit log (who, what, when, old value, new value).

---

## 13. Phase Split

### Phase 1a — Core (Week 1-2)

**Entities:** Patient + Owner + Anamnese + Consultation + Vaccination

**Deliverables:**
- Domain models (5 entities)
- SQLDelight schema + `.sq` files
- Repositories + use cases
- ViewModels + screens
- Navigation routes
- Koin modules
- Tests (repo + use case + VM)

**Why split:** 18+ entities in one phase is unrealistic. Core workflow (select horse → record consultation → track vaccination) is the minimum viable app. Prove architecture with 5 entities before scaling.

### Phase 1b — Remaining Entities (Week 3-4)

**Entities:** All 14 remaining

**Deliverables:**
- Domain models (14 entities)
- SQLDelight schema + `.sq` files
- Repositories + use cases
- ViewModels + screens
- Navigation routes
- Tests

**Why batch:** Same pattern as 1a. Mechanical implementation. Can parallelize across multiple fixer agents.

### Phase 2 — Search

**Deliverables:**
- FTS5 virtual table
- Search use case
- Search screen with grouped results
- Navigation to PatientDetail (scroll to source)

### Phase 3 — Export, Backup, PDF

**Deliverables:**
- JSON export
- Raw DB backup
- PDF generation (iText/PDFKit)
- Manual backup/restore UI

### Phase 4 — Notifications

**Deliverables:**
- Platform-native notification scheduling
- Reminder calculation use cases
- Notification settings screen

### Phase 5 — Polish

**Deliverables:**
- Responsive layout
- Theme system
- Accessibility
- Error handling

### Phase 6 — Cloud Sync

**Deliverables:**
- Cloud backup
- Cross-device sync
- UUID migration (if needed)
- Auth (if multi-user)

---

## 14. Non-Goals (Phase 1-3)

- **Multi-user / multi-vet:** Single vet, single phone. No auth, no user accounts.
- **Cloud sync:** Deferred to Phase 6. No network code in Phase 1-3.
- **Regulatory compliance:** Controlled substance audit deferred until sharing with 3rd parties.

---

## 15. ADR Index

22 ADRs in `docs/adr/`. All decisions documented with Context, Decision, Alternatives, Consequences.

| ADR | Decision | Key rationale |
|-----|----------|---------------|
| 0001 | KMP + Compose Multiplatform | Shared UI across platforms |
| 0002 | SQLDelight over Room | Only KMP-compatible DB |
| 0003 | Koin over Hilt | Only KMP-compatible DI |
| 0004 | Navigation 3 | Type-safe routes, KMP support |
| 0005 | Auto-increment IDs | Defer UUID/sync complexity |
| 0006 | Tabbed PatientDetail | 18+ entities need grouping |
| 0007 | Internal storage + backup | Platform storage restrictions |
| 0008 | Controlled substance CRUD | Personal app, lenient for now |
| 0009 | Instant→Long, LocalDate→String | Standard KMP pattern |
| 0010 | App-layer FK enforcement | No DB cascade accidents |
| 0011 | Patient-centric FTS5 | One search query, all matches |
| 0012 | Platform-native notifications | Full control, no library risk |
| 0013 | JVM PDF + PDFKit iOS | Platform-specific, expect/actual |
| 0014 | JSON + raw DB backup | Portable + fast options |
| 0015 | Phase 1 split (1a + 1b) | Prove architecture, then scale |
| 0016 | Hybrid Koin modules | Annotations + explicit bindings |
| 0017 | Explicit dispatchers | Predictable, familiar pattern |
| 0018 | SQLDelight migrations from day 1 | Safe schema evolution |
| 0019 | Three test targets | Full platform coverage |
| 0020 | In-memory DB + mock doubles | Real tests + isolated logic |
| 0021 | Soft delete via isActive | Preserve medical records |
| 0022 | Fallback to backup | Recovery on migration failure |

---

## 16. Suggested Next Command

```
/deepwork "Implement Phase 1a: Patient + Owner + Anamnese + Consultation + Vaccination with domain models, SQLDelight schema, repos, use cases, ViewModels, screens, navigation routes, Koin modules, and tests"
```
