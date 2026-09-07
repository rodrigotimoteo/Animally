# Maintenance Quality Sweep — 2026-09-07

## Change contract

Improve the current Animally product through evidence-backed vertical slices: eliminate confirmed correctness/lifecycle defects, strengthen edge-case and integration coverage, make UI tests deterministic, and remove duplication only after parity behavior is protected. Keep business rules in `commonMain`, keep SwiftUI/platform code at the edges, preserve soft-delete/migration/cancellation semantics, and do not commit credentials or silently choose unresolved backup/cloud/privacy policy.

Allowed work surfaces are `shared`, `androidApp`, `iosApp`, `scripts`, maintained verification/docs, and focused test fixtures. Existing unrelated working-tree edits in `.codex/config.toml` and `AGENTS.md` remain untouched. Non-goals are speculative rewrites, new production dependencies, unsupported clinical metrics, and enabling dormant cloud capabilities without a product contract.

Acceptance is evidence-based: every changed behavior has a focused regression test where practical; shared Android-host, desktop, and iOS simulator tests remain green; static analysis and Android packaging remain green; the iOS host builds; simulator/UI flows are either exercised with recorded evidence or documented with an exact environmental blocker; and the final diff removes dead code/duplicate paths rather than adding scaffolding.

## Sweep work order

### Phase 0 — Baseline and inventory

- [ ] Re-run the current shared/platform quality matrix and record the exact baseline.
- [ ] Reconcile `docs/maintenance/TECH_DEBT_PLAN.md`, `tasks/todo.md`, README verification commands, and actual source/test coverage.
- [ ] Produce a feature matrix covering records, search/FTS, backup/restore/wipe, sync, reminders, exports, assistant, dictation, settings, navigation, and native bridges.

### Phase 1 — Assistant and dictation vertical slice

- [ ] Trace assistant request, streaming, tool/RAG, error, cancellation, view disappearance, and retry paths across shared Kotlin and iOS/Android bridges.
- [ ] Make cancellation ownership explicit and test no late result/error can overwrite a newer request or a disposed screen.
- [ ] Exercise dictation capture, cancel, transcribe, extraction, review, save, audio playback, permission denial, unsupported-device, empty transcript, malformed extraction, and repeated-submit paths.
- [ ] Add focused shared/iOS regression tests and simulator/UI evidence for the user-visible states.

### Phase 2 — Local data safety and lifecycle

- [ ] Add migration upgrade fixtures for the supported SQLDelight migration chain and test failure/recovery semantics without inventing a recovery policy.
- [ ] Verify restore preflight, media portability/deletion boundaries, raw snapshot semantics, wipe residual reporting, and backup privacy as explicit contracts; implement only deterministic defects that do not require a product decision.
- [ ] Verify reminder scheduling, permission loss, disable/delete/wipe cancellation, and idempotent recreation across Android/iOS/desktop.

### Phase 3 — Platform reachability and UI integration

- [ ] Make iOS UI tests deterministic and isolated; remove paused/fixture-dependent tests and add stable seeding/reset.
- [ ] Run the simulator helper against a real available device for launch, settings, backup/restore, search, assistant, dictation, navigation, theme, Dynamic Type, and accessibility checks; preserve exact blockers when CoreSimulator/Xcode prevents execution.
- [ ] Verify Android route reachability, notification permission prompts, sharing context, compact settings scrolling, and capability claims with host/runtime evidence.

### Phase 4 — Cross-projection parity

- [ ] Compare record-family coverage across repositories, soft-delete/change tracking, sync payloads/handlers, search/FTS, timeline, exports, backup, and native routing.
- [ ] Add golden fixtures for empty/zero-denominator/inactive/unknown-category/partial-date/duplicate-submit cases and assert source traceability for assistant answers.
- [ ] Consolidate only duplicated metadata or mapping that is covered by parity tests; delete superseded helpers and stale suppressions.

### Phase 5 — Release confidence and cleanup

- [ ] Add one offline aggregate verification gate covering tests, lint/Detekt, coverage, Android packaging, iOS shared compilation, Xcode host build, script contracts, and available UI tests.
- [ ] Refresh stale architecture/structure/maintenance documentation from measured state and record unresolved product-policy decisions explicitly.
- [ ] Run a final reviewer pass on the actual diff, then perform a deletion/simplification pass before committing the batch.

## Checkpoints

- **Checkpoint A:** baseline matrix and feature inventory are current; no source edits begin before this is recorded.
- **Checkpoint B:** assistant/dictation behavior has focused regression coverage and no unresolved cancellation ownership.
- **Checkpoint C:** local data and platform UI gates pass; simulator evidence or an exact blocker is recorded.
- **Checkpoint D:** full matrix, final reviewer, deletion pass, and clean scoped commit pass.

## Risks and explicit decisions

| Risk | Impact | Handling |
|---|---|---|
| Backup media portability and sensitive assistant/dictation backup policy remain product decisions | High | Measure and document current behavior; do not silently change retention or encryption semantics. |
| CloudKit account/rejoin/wipe behavior depends on entitlement and live accounts | High | Keep deterministic reset/cursor tests; require live capability evidence before enabling or advertising more. |
| Simulator/CoreSimulator availability may block UI evidence | Medium | Verify Xcode/runtime/device health first; separate compile evidence from UI evidence. |
| Broad record parity can create a parallel source of truth | High | Reuse existing registries and add parity tests before consolidation. |

---

# Implementation Plan: Internship Insights Dashboard

## Overview

Build an iOS-first, cross-platform insights feature that turns Animally's existing clinical and reproduction records into an auditable internship overview and research-ready export. Every count and chart is calculated deterministically in shared Kotlin from persisted records; Swift renders the native iOS experience but owns no metric logic. Every dashboard value can be opened to reveal the records behind it.

The first release deliberately reports facts such as activity counts, case mix, breeding events, active gestations, embryo collections, and ICSI totals. It must not claim conception, transfer-success, or foaling-success rates until the data model can link attempts to outcomes with an explicit denominator.

## Agent Handoff Contract

The next implementation agent should start from a clean worktree, read `AGENTS.md`, `ARCHITECTURE.md`, and `STRUCTURE.md`, and run the shared baseline gate before editing:

```bash
git status --short
./gradlew :shared:testAndroidHostTest \
  :shared:iosSimulatorArm64Test \
  :shared:ktlintCheck \
  :shared:detekt
```

The approved first milestone is **Tasks 1–9: Overview MVP with source drill-down**. Tasks 10–13 are the next planned extension and must not delay a working overview. Keep every task independently buildable and commit at the end of each completed work session.

### Locked decisions

- Default range: trailing 30 calendar days, inclusive of today.
- Initial presets: 30 days, 90 days, all time, and custom. “Current semester” waits for the actual programme dates.
- Initial scopes: all active patients or one active patient. Owner and saved-cohort scopes are follow-ups.
- No sixth iOS root tab. Enter global Insights from Timeline and patient Insights from Patient Detail.
- No production chart dependency. Use Swift Charts on iOS 18.2+; Compose presentation can follow later.
- No LLM involvement in calculation, classification, comparison, filtering, or export.
- No outcome-linkage schema migration in the Overview MVP.
- No opaque quality or performance score. Show defined counts and explicit denominators.
- Current-state cards use one injected `today` captured for the load; they are not constrained by the historical range.
- Pseudonyms in the first export are bundle-local `P001`, `P002`, and so on, assigned by ascending patient ID. Persistent study identities belong to the saved-cohort follow-up.

### Non-goals for the first milestone

- Conception, pregnancy, embryo-transfer, treatment-success, or live-foal rates.
- Inferred visits, internship hours, competencies, diagnoses, outcomes, or causal conclusions.
- Cloud sync for filters, dashboard cache tables, or precomputed analytics.
- Replacing Timeline, Search, or existing patient record screens.

## Product Principles

- **Auditable:** tapping a metric reveals its source records and active filters.
- **Deterministic:** the LLM never calculates dashboard values.
- **Clinically honest:** counts are not relabelled as outcomes or success rates.
- **Research friendly:** exports include filters, definitions, generated time, and a data dictionary.
- **Private by default:** thesis exports can replace names with stable study identifiers and exclude owner contact data.
- **iOS native, logically shared:** Swift Charts and SwiftUI render shared Kotlin state.

## Proposed Experience

Do not add a sixth tab to the existing five-item iOS tab bar. Add an Insights button to Timeline and a patient-scoped Insights action to Patient Detail. A future iteration may rename Timeline to Activity and use a Timeline/Insights segmented control.

The dashboard has four sections:

1. **Internship overview** — patients seen, recorded activities, active clinical days, average activities per active day, and period-over-period change.
2. **Case mix** — activity trend and record-type distribution, with drill-down to the underlying records.
3. **Reproduction** — breeding and pregnancy-check events in the selected period, embryo/ICSI totals, plus a separately labelled current gestation and due-soon snapshot.
4. **Research readiness** — explicit data-quality issue counts, anonymised export, and definitions. Avoid a vague single “quality score.”

### Filters

- Date presets: 30 days, 90 days, custom, and all time.
- Scope: all active patients or one active patient.
- Comparison: the immediately preceding equal-length range for 30-day, 90-day, and valid custom ranges; disabled for all time.
- Record type is selected by tapping a case-mix segment and applies only to the drill-down list in the first milestone.

## Exact Metric Semantics

- A **recorded activity** is one active row from the included source matrix below. It is not necessarily a consultation, visit, or procedure.
- A **case-day** is one unique `(patientId, date)` pair containing at least one recorded activity.
- **Patients seen** is the distinct patient count across recorded activities. It does not mean newly registered patients.
- An **active day** is a distinct date containing at least one recorded activity.
- **Activities per active day** is `activityCount / activeDayCount`, or unavailable when the denominator is zero.
- **Activities per case-day** is `activityCount / caseDayCount`, or unavailable when the denominator is zero.
- **Record share** is `recordTypeCount / activityCount`, or unavailable for an empty period.
- All date ranges are inclusive: `from <= recordDate <= to`.
- For an inclusive range of `N` days, the comparison range is the `N` days ending on `from - 1 day`.
- Daily buckets are used through 45 days, calendar-week buckets from 46–180 days, and calendar-month buckets above 180 days.
- Global metrics only include rows linked to an active patient and with `isActive = 1` on the source row.
- All-time resolves from the earliest included activity date through the captured `today`; an empty database produces an empty state, not an invented range.

### Activity source matrix

| Record type | Date column | Included in period activity | Notes |
| --- | --- | --- | --- |
| Consultation | `date` | Yes | One row is one activity, regardless of SOAP length. |
| Dentistry | `date` | Yes |  |
| Deworming | `dateAdministered` | Yes |  |
| Farrier visit | `date` | Yes |  |
| Imaging | `date` | Yes |  |
| Lab result | `date` | Yes |  |
| Lameness | `date` | Yes |  |
| Medication | `startDate` | Yes, when non-null | A prescription without a start date has no defensible activity date. |
| Reproduction event | `date` | Yes | Category is canonicalised in shared Kotlin. |
| Reproduction medication | `dateAdministered` | Yes |  |
| Surgery | `date` | Yes |  |
| Controlled substance | `date` | Yes |  |
| Ultrasound | `date` | Yes | Child follicle rows are not counted separately. |
| Vaccination | `dateAdministered` | Yes |  |
| Weight | `date` | Yes |  |
| Embryo transfer | `date` | Yes | Counted as donor-side activity with the current model. |
| ICSI | `date` | Yes |  |
| Gestation | `breedingDate` | No | A longitudinal/current state that may duplicate a breeding event. |
| Anamnese | none | No | No natural activity date. |
| Custom reminder | due date | No | A planned task is not completed clinical activity. |
| Follicle | parent ultrasound | No | Child measurement; count the ultrasound once. |

## Metrics That Are Defensible Today

| Metric | Definition | Existing source |
| --- | --- | --- |
| Patients seen | Distinct patients with at least one active dated record in range | Existing patient-linked record tables |
| Recorded activities | Count of active dated records in range; never labelled “procedures” | Existing record tables |
| Case-days | Distinct patient/date pairs with at least one recorded activity | Existing record dates |
| Active days | Distinct dates with at least one activity | Existing record dates |
| Activities per active day | Recorded activities divided by active days | Derived in shared use case |
| Activities per case-day | Recorded activities divided by case-days | Derived in shared use case |
| Activity trend | Activity count grouped by day/week | Existing record dates |
| Record mix | Count and percentage by `RecordType` | Existing record tables |
| Reproduction event mix | Heat, breeding, pregnancy-check, foaling, and initial-exam counts | `Reproduction.eventType` after canonical parsing |
| Active gestations | Active, unresolved gestation records as of today | `Gestation` |
| Due soon | Active gestations due in 30/60/90 days | `Gestation.expectedDueDate` |
| Embryos collected | Sum and average `embryoCount` per collection in range | `EmbryoTransfer` |
| ICSI activity | Session count and follicles recovered in range | `Icsi` |
| Ultrasound activity | Examination count and structured follicle measurements | `Ultrasound` |

## Metrics Requiring Better Linkage

Do not expose these as rates in the MVP:

- Conception/pregnancy rate: a gestation or pregnancy check is not linked to a specific breeding attempt or cycle.
- Embryo-transfer success: recipient mares are free text and have no linked recipient outcome.
- Foaling/live-foal rate: foaling events lack structured outcome fields.
- Clinical treatment effectiveness: diagnoses, treatments, and outcomes are not structured as linked episodes.

Phase 2 can add `reproductionCycleId`, `sourceBreedingEventId`, structured pregnancy-check outcomes, transfer recipients, and foaling outcomes. Existing rows remain valid and appear as “outcome unavailable,” never guessed.

## Architecture

```text
Existing SQLDelight record tables
              │
              ▼
data/insights/Insights.sq + SqlDelightInsightsRepository
              │ returns aggregate facts, not presentation strings
              ▼
domain/insights/GetInsightsDashboardUseCase
              │ applies taxonomy, rates, comparisons, and caveats
              ▼
presentation/insights/InsightsViewModel + InsightsUiState
              │ StateFlow / Objective-C export
        ┌─────┴────────┐
        ▼              ▼
SwiftUI + Charts    Compose presentation later
        │
        └── drill-down routes to existing record editors
```

### Shared Models

- `InsightsFilter(from, to, patientId)`; ranges are resolved and always valid before repository access.
- `InsightsSnapshot(overview, activitySeries, recordMix, reproduction, currentCare, dataIssues)`
- `OverviewMetrics(patientCount, activityCount, caseDayCount, activeDayCount, averagePerActiveDay, averagePerCaseDay, comparison)`
- `MetricComparison(current, previous, absoluteDelta, percentageDelta?)`
- `ActivityPoint(periodStart, count)`
- `RecordTypeCount(type, count, share)`
- `ReproductionMetrics(eventCounts, embryoCollections, embryosCollected, icsiSessions, folliclesRecovered)`
- `CurrentGestationItem(patientId, patientName, gestationDay, dueDate, daysUntilDue, status)`
- `InsightsRecordRef(recordType, patientId, recordId, patientName, date)`
- `InsightsDrillDown(recordType?, from, to, patientId?)`

Prefer concrete data classes over a generic chart DSL so Objective-C/Swift interop stays predictable.

### Query Strategy

- Add focused aggregate SQLDelight queries rather than loading every record into memory.
- Use `UNION ALL` over active dated tables for activity and record-mix facts.
- Apply identical inclusive date and patient filters to every branch.
- Keep period metrics and the current operational snapshot separate; due-soon is not silently constrained by a historical date filter.
- Add indexes only after an explain-plan/performance test shows a need.
- Preserve soft-delete semantics (`isActive = 1`) everywhere.

### Reproduction Taxonomy

The code currently persists both `PregnancyCheck` and `Pregnancy Check` depending on platform. Introduce a shared `ReproductionEventType` with a stable wire value and display label. Its parser accepts legacy spellings, while all new saves use the stable wire value. This is required before charting event categories.

Use canonical storage labels `Heat`, `Breeding`, `Pregnancy Check`, `Foaling`, and `Initial Exam`. Parsing should be case-insensitive and ignore spaces, underscores, and hyphens so historical `PregnancyCheck` remains valid. Unknown values map to the dashboard's `Other` category while the original record text remains untouched.

## Expected File Map

The names below are the intended placement, not an invitation to create all files at once.

```text
shared/src/commonMain/
├── kotlin/com/github/rodrigotimoteo/animally/
│   ├── domain/reproduction/model/ReproductionEventType.kt
│   ├── domain/insights/
│   │   ├── IInsightsRepository.kt
│   │   ├── model/InsightsFilter.kt
│   │   ├── model/InsightsSnapshot.kt
│   │   └── usecase/GetInsightsDashboardUseCase.kt
│   ├── data/insights/SqlDelightInsightsRepository.kt
│   ├── presentation/insights/InsightsViewModel.kt
│   └── di/presentation/InsightsPresentationModule.kt
└── sqldelight/com/github/rodrigotimoteo/animally/data/insights/Insights.sq

shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/
├── presentation/ios/InsightsStore.kt
└── di/infra/IosInsightsStores.kt

iosApp/iosApp/Insights/
├── InsightsView.swift
├── InsightsViewModel.swift
├── InsightsOverviewSection.swift
├── InsightsCaseMixSection.swift
└── InsightsRecordsView.swift
```

Tests mirror production packages under `shared/src/commonTest`, `shared/src/androidHostTest`, and `shared/src/iosTest`. Add the new presentation module to `PresentationModule`; do not place dashboard construction into an unrelated settings or patient module. `IosInsightsStores` should follow `IosSettingsStores.timelineStore`, and `InsightsStore` should follow `TimelineStore`/`NativeFlow` rather than introducing a second observation bridge.

For source navigation, reuse `RecordDetailKey`, `RecordDetailNav`, and `RecordDetailView`. Do not add another raw-string-to-editor mapping; existing `RecordEditRoute`/record detail handling already owns that boundary.

### SQLDelight query contract

Implement one `WITH activity_rows AS (...)` union in each required named query; do not add an analytics cache table or migration for the MVP.

- `selectActivityBuckets(from, to, patientId)` returns `date`, `patientId`, `recordTypeWireName`, and `count` grouped by those columns.
- `selectActivityRecordRefs(from, to, patientId, recordTypeWireName)` returns the underlying `recordId`, `patientId`, `patientName`, `date`, and record type for drill-down.
- `selectEarliestActivityDate(patientId)` resolves all-time range.
- Reproduction/current-gestation queries are added only in Tasks 9–10.

Pass `patientId` as nullable and use `(:patientId IS NULL OR source.patientId = :patientId)` consistently. Join `Patient` once outside the union where possible and require `Patient.isActive = 1`. Use stable `RecordType.wireName` literals in SQL and parse them with `RecordType.fromWireName` in the repository.

## Task List

### Phase 1: Data vocabulary and contracts

#### Task 1: Add reproduction event parsing

**Description:** Introduce `ReproductionEventType` with canonical storage/display labels and tolerant legacy parsing. Do not rewrite historical rows.

**Acceptance criteria:**

- `PregnancyCheck`, `Pregnancy Check`, `pregnancy_check`, and `pregnancy-check` resolve to one category.
- Every known picker value round-trips; unknown text resolves to `Other` while the raw model value survives.
- No platform UI or persistence behaviour changes in this task.

**Verification:** `./gradlew :shared:testAndroidHostTest --tests '*ReproductionEventTypeTest*'`

**Dependencies:** None

**Files likely touched:** `ReproductionEventType.kt`, `ReproductionEventTypeTest.kt`.

**Estimated scope:** Small

#### Task 2: Canonicalise newly saved reproduction values

**Description:** Normalise selected event values in the shared edit ViewModel and align the Compose picker label. Keep the iOS picker displaying labels, not storage internals.

**Acceptance criteria:**

- New records from both hosts persist the canonical labels.
- Existing legacy records load and edit without changing category unless saved.
- List/detail screens display human labels and never `PREGNANCY_CHECK`-style internals.

**Verification:** Focused `ReproductionEventEditViewModelTest`, iOS simulator compilation, and manual edit of one legacy fixture row.

**Dependencies:** Task 1

**Files likely touched:** `ReproductionEventEditViewModel.kt`, `ReproductionEventEditScreen.kt`, `ReproductionEventEditView.swift`, focused tests.

**Estimated scope:** Medium

#### Task 3: Define the overview domain contract

**Description:** Add concrete, Swift-friendly filter, facts, snapshot, comparison, drill-down, and repository types. Keep formatting strings out of repository facts.

**Acceptance criteria:**

- Invalid ranges cannot reach `IInsightsRepository`.
- Empty periods and unavailable percentages are represented explicitly.
- `:shared:compileKotlinIosSimulatorArm64` exports the models without unsupported generic/collection shapes.

**Verification:** Common model tests and `./gradlew :shared:compileKotlinIosSimulatorArm64`.

**Dependencies:** Task 1

**Files likely touched:** `domain/insights/model/InsightsFilter.kt`, `InsightsSnapshot.kt`, `IInsightsRepository.kt`, tests.

**Estimated scope:** Medium

### Checkpoint A: Contracts

- [ ] Tasks 1–3 focused tests pass.
- [ ] Android host and iOS simulator Kotlin compilation pass.
- [ ] No database migration or LLM dependency was introduced.

### Phase 2: Deterministic overview vertical slice

#### Task 4: Implement activity queries and repository

**Description:** Add the activity union queries described above and map generated SQLDelight rows into domain facts.

**Acceptance criteria:**

- Every included source type contributes exactly once; excluded types never contribute.
- Soft-deleted rows, rows linked to inactive patients, out-of-range rows, and null medication start dates are excluded.
- Record-type counts sum exactly to the total activity count for global and patient scope.

**Verification:** `./gradlew :shared:testAndroidHostTest --tests '*SqlDelightInsightsRepositoryTest*'`

**Dependencies:** Task 3

**Files likely touched:** `Insights.sq`, `SqlDelightInsightsRepository.kt`, repository mapper, Android-host integration test.

**Estimated scope:** Medium

#### Task 5: Calculate overview snapshots

**Description:** Build `GetInsightsDashboardUseCase` for resolved ranges, comparison ranges, bucket granularity, case-days, safe averages/shares, and all-time empty handling. Inject `todayProvider` for deterministic tests.

**Acceptance criteria:**

- The definitions in “Exact Metric Semantics” are executable test cases.
- Zero denominators yield `null`/unavailable values, never zero, infinity, or NaN percentages.
- Current and comparison repository calls use exact non-overlapping inclusive ranges.

**Verification:** `./gradlew :shared:testAndroidHostTest --tests '*GetInsightsDashboardUseCaseTest*'`

**Dependencies:** Task 4

**Files likely touched:** `GetInsightsDashboardUseCase.kt`, date-bucketing helper, use-case tests.

**Estimated scope:** Medium

#### Task 6: Add shared presentation state

**Description:** Add `InsightsViewModel`, explicit loading/content/empty/error state, cancellable reloads, presets, patient scope, and custom-range validation. Register it in a dedicated presentation module.

**Acceptance criteria:**

- Rapid filter changes cannot publish an older request over a newer one.
- The first load defaults to the locked 30-day range; invalid custom input stays visible with validation feedback.
- ViewModel work runs on the injected IO dispatcher and exposes immutable `StateFlow`.

**Verification:** `./gradlew :shared:testAndroidHostTest --tests '*InsightsViewModelTest*'`

**Dependencies:** Task 5

**Files likely touched:** `InsightsViewModel.kt`, `InsightsPresentationModule.kt`, `PresentationModule.kt`, ViewModel tests.

**Estimated scope:** Medium

#### Task 7: Add the iOS observation bridge

**Description:** Wrap the shared ViewModel with `InsightsStore`/`NativeFlow` and expose construction through `IosInsightsStores`.

**Acceptance criteria:**

- Swift can observe state and invoke reload, preset, custom-range, patient, and error-dismiss actions.
- Store code contains no calculation, date arithmetic, formatting policy, or database access.
- An iOS bridge test observes the initial and loaded states and cancels cleanly.

**Verification:** `./gradlew :shared:iosSimulatorArm64Test --tests '*InsightsStoreTest*'`

**Dependencies:** Task 6

**Files likely touched:** `InsightsStore.kt`, `IosInsightsStores.kt`, `InsightsStoreTest.kt`.

**Estimated scope:** Medium

### Checkpoint B: Headless vertical slice

- [ ] A fixture produces the exact expected snapshot through the iOS store.
- [ ] Full shared Android-host and iOS-simulator tests pass.
- [ ] Dashboard calculations exist only in shared Kotlin.

### Phase 3: iOS overview MVP

#### Task 8: Render overview and case mix

**Description:** Build the Swift wrapper and SwiftUI screen with summary cards, activity chart, record-mix chart/list, range controls, patient label, and polished state handling.

**Acceptance criteria:**

- Content, loading, empty, validation, and retry states are legible in light/dark/system themes.
- Charts use accessible labels plus textual values and do not communicate category solely by colour.
- Compact width and large Dynamic Type do not clip cards, filters, chart labels, or values.

**Verification:** Xcode simulator build; fixture screenshots in light/dark and default/AX3 Dynamic Type; VoiceOver inspection.

**Dependencies:** Task 7

**Files likely touched:** `InsightsViewModel.swift`, `InsightsView.swift`, `InsightsOverviewSection.swift`, `InsightsCaseMixSection.swift`.

**Estimated scope:** Medium

#### Task 9: Wire entry points and source drill-down

**Description:** Add global entry from Timeline, patient-scoped entry from Patient Detail, and a filtered record-reference list that reuses existing record detail navigation.

**Acceptance criteria:**

- No sixth tab is added and Back returns to the originating screen.
- Global and patient entry points open with the correct scope.
- Tapping a case-mix category shows matching source rows; tapping a row opens `RecordDetailView` through existing navigation types.

**Verification:** Simulator walkthrough from both entry points and at least three record types; add/update iOS UI smoke coverage if stable identifiers permit.

**Dependencies:** Tasks 4 and 8

**Files likely touched:** `TimelineView.swift`, `PatientDetailView.swift`, `InsightsRecordsView.swift`, Insights navigation state/tests.

**Estimated scope:** Medium

### Checkpoint C: Approved Overview MVP

- [ ] Daniela can answer “how much did I record, on how many horses/days, and what kind of work?” for a chosen range.
- [ ] Every displayed metric matches a golden fixture and every case-mix value drills down.
- [ ] No unsupported clinical or reproduction outcome is displayed.
- [ ] Shared gate, Xcode build, theme/accessibility checks, and simulator smoke test pass.
- [ ] Commit the Overview MVP with a clean worktree before starting extensions.

### Phase 4: Reproduction and thesis extensions

#### Task 10: Add reproduction-period facts

**Description:** Extend the repository and snapshot with canonical event counts, embryo collections/embryos, ICSI sessions/follicles, and ultrasound counts.

**Acceptance criteria:**

- Unknown event values appear under `Other`; soft-deleted and out-of-range rows are excluded.
- Sums and averages include explicit sample counts and unavailable zero-denominator states.
- No conception, transfer-success, or live-foal rate is introduced.

**Verification:** `./gradlew :shared:testAndroidHostTest --tests '*InsightsReproductionTest*'`

**Dependencies:** Tasks 1, 4, and 5

**Files likely touched:** `Insights.sq`, repository/snapshot reproduction models, use case, golden tests.

**Estimated scope:** Medium

#### Task 11: Add current gestation operations

**Description:** Add active gestation and due-soon groups using the captured `today`, independently from the historical period filter.

**Acceptance criteria:**

- Gestation day is recalculated from breeding date rather than trusting stored `gestationDays`.
- Inactive/resolved gestations are excluded and overdue items are explicit.
- Each row links to the mare and gestation source record.

**Verification:** Boundary tests for today, overdue, leap dates, resolved statuses, and 30/60/90-day groups.

**Dependencies:** Tasks 5 and 9

**Files likely touched:** Gestation query/model/use-case extension, tests, `InsightsGestationSection.swift`.

**Estimated scope:** Medium

#### Task 12: Export a dashboard-aligned analysis bundle

**Description:** Export summary, activity series, record mix, reproduction metrics, and a data dictionary using the exact loaded snapshot/filter. Add optional bundle-local pseudonyms.

**Acceptance criteria:**

- Export records range, scope, generation time, app/schema version, and metric definitions.
- Dashboard totals and CSV totals are byte-for-byte derived from the same snapshot values.
- Pseudonymised mode contains no patient names or owner contact/location fields.

**Verification:** Golden CSV tests, privacy allowlist test, quoted/newline round-trip test, and iOS share-sheet smoke test.

**Dependencies:** Tasks 9–11

**Files likely touched:** `ExportInsightsBundleUseCase.kt`, exporter/data dictionary, tests, iOS share action.

**Estimated scope:** Medium

#### Task 13: Add research-readiness drill-down

**Description:** Report concrete issue counts for unknown reproduction categories, missing vet names, unlinked owners, free-text embryo recipients, and incomplete structured ultrasound data.

**Acceptance criteria:**

- Each rule has a written definition and opens only the affected source rows.
- UI language says “missing for analysis,” not “clinically wrong.”
- No combined quality score or LLM classification is used.

**Verification:** Rule unit tests, repository fixture tests, and simulator drill-down walkthrough.

**Dependencies:** Tasks 9–11

**Files likely touched:** Insights data-quality rules/models, shared state, tests, iOS readiness section.

**Estimated scope:** Medium

### Checkpoint D: Complete dashboard

- [ ] Full shared tests, Detekt, KtLint, and Xcode simulator build pass.
- [ ] Dashboard and export agree on every fixture total.
- [ ] Privacy review confirms no owner-identifying data in pseudonymised exports.
- [ ] Physical-device verification passes before release installation.
- [ ] Commit with a clean worktree.

## Product Backlog Relationship

Ideas intentionally outside this implementation are tracked in [`docs/FEATURE_IDEAS.md`](../docs/FEATURE_IDEAS.md). Promote an idea into this plan only after the current milestone is complete or the user explicitly changes scope.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Different platforms persist different category strings | High | Shared canonical taxonomy with legacy parser before aggregation. |
| Counts accidentally include soft-deleted rows | High | Shared SQL fixture with active/inactive pairs for every table. |
| Dashboard implies causality or clinical outcomes | High | Deterministic definitions, explicit denominators, no unsupported rates. |
| Large databases make aggregation slow | Medium | Aggregate in SQL, benchmark fixtures, add indexes only with evidence. |
| Date filters disagree across sections | Medium | One `InsightsFilter`, inclusive range semantics, contract tests. |
| Thesis export leaks personal data | High | Pseudonymised mode, allowlist exported fields, privacy regression tests. |
| Six-tab iOS navigation becomes crowded | Medium | Enter Insights from Timeline and Patient Detail; keep five root tabs. |

## Execution and Parallelisation

- Tasks 1 and 3 may be implemented in parallel after their shared naming is agreed; Task 2 follows Task 1.
- Tasks 4–7 are sequential because each defines the next contract boundary.
- After Task 7, SwiftUI rendering fixtures and shared drill-down repository tests may proceed in parallel, but Task 9 integration waits for Task 8.
- Tasks 10 and 11 may proceed in parallel after the Overview MVP checkpoint.
- Task 12 waits for stable snapshot models from Tasks 10–11. Task 13 may proceed alongside Task 12.
- Parallel write lanes must use separate worktrees; the integrating agent reviews tests and architecture before merging.

## Definition of Done

For each task:

1. Add focused behavioural tests before or with the implementation.
2. Run the narrow task-specific command listed in the task.
3. Run `./gradlew :shared:ktlintCheck :shared:detekt :shared:compileKotlinIosSimulatorArm64` before handoff.
4. For Swift changes, run an Xcode simulator build and inspect the changed flow in the simulator.
5. Review `git diff --check`, verify no unrelated changes, and commit with a clean worktree.

At Checkpoints C and D, run the complete shared gate from the handoff contract plus:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build
```

## Deferred Product Inputs

These do not block the approved Overview MVP:

- Semester dates: omit that preset until Daniela supplies the programme boundaries.
- Official hours/competencies/signatures: implement as explicit internship-session data, tracked in the feature backlog; never infer them from patient records.
- Patient eligibility: patient-scoped Insights is available for every active patient, not only reproduction cases.
- Persistent pseudonyms: bundle-local pseudonyms are the first version; stable cross-export study identities wait for saved cohorts.

## Follow-up UI polish: playback, export theme, and insight cards

### Behaviors that must become true

- Playback-speed choices remain readable in the confirmation sheet in both light and dark appearances, including the selected option.
- The PDF export uses the currently selected accent palette when it is generated, while retaining a readable light document and the existing report content/layout.
- Insight stat cards have a consistent default height, and readiness rows keep their count/chevron visible while allowing explanatory copy to wrap.
- The overview heading is user-facing neutral language: “Activity overview”.

### Behaviors that must remain true

- Playback rate values, seeking, stop behavior, and accessibility identifiers do not change.
- PDF Android/iOS renderers continue to consume the same platform-neutral operations and existing default reports remain forest-themed.
- Insight counts, filters, drill-down actions, and unsupported-rate caveats remain unchanged.
- Accessibility text remains available; larger text may grow cards/rows rather than clipping content.

### Failure modes and verification

- Verify shared PDF layout tests and a new palette assertion, then compile the iOS simulator target.
- Run the iOS UI test target/build when Xcode tooling is available; otherwise report that visual simulator evidence is unavailable.
- Review the diff for accidental metric/string changes and run `git diff --check`.

## Current request: dictation extraction and medical reference coverage

### Task A — Expand safe medical-term recognition

- **Goal:** make assistant reference cards appear for the full supported veterinary/medical vocabulary, including common transcription misspellings.
- **Acceptance criteria:** exact known terms and bounded one-edit misspellings canonicalise to the same topic; only canonical topics are sent to trusted-source search; patient-specific or otherwise unsafe prompts do not become public search queries; focused matcher and reference-provider tests pass.
- **Verification:** run the veterinary query/reference tests, then shared lint and relevant compilation.
- **Dependency:** none. This is the first checkpoint.

### Task B — Make dictation preserve every supported record

- **Goal:** retain medication records in the structured dictation contract and resolve a uniquely spoken patient-name prefix such as “Descarada” to “Descarada do Monte Alto”.
- **Acceptance criteria:** the screenshot transcript can yield both an ultrasound and a medication suggestion; medication fields validate and persist through the existing medication save path; ambiguous or unsafe patient matches remain for user review; focused DTO/validation/insertion/patient-resolution tests pass.
- **Verification:** run all focused dictation tests and the shared Android-host test target.
- **Dependency:** Task A can be verified independently; this task is the second checkpoint.

### Task C — Repair dictation review presentation

- **Goal:** show the finalized transcript during extraction and give the review editor/action area a deliberate, usable size.
- **Acceptance criteria:** extraction preview contains the complete finalized transcript rather than the last partial speech fragment; the transcript editor no longer consumes the whole sheet on a short transcript; accessibility labels and extraction behavior remain intact.
- **Verification:** build the iOS app target and install the verified build on Rodrigo’s paired iPhone when the build succeeds. Simulator/physical visual inspection is reported separately from compilation evidence.
- **Dependency:** Task B’s expanded contract must compile before the iOS build checkpoint.

### Risks and mitigations

- **Fuzzy matching false positives:** use bounded edit distance and a curated vocabulary; fail closed for unknown terms and never forward raw patient-bearing text.
- **Model omission or hallucination:** require exact JSON fields, validate each record structurally, and preserve the transcript for correction before extraction.
- **New medication persistence path:** reuse the existing `Medication` model and `SaveMedicationUseCase`; add focused tests before changing UI wiring.

## Current request: generated trusted medical vocabulary

### Architecture decision

Use the current NLM Medical Subject Headings (MeSH) descriptor dataset as a
build-time input. Generate a compact local index from disease and drug
descriptors, including entry terms, and keep the existing runtime privacy gate
and trusted-source relevance filter. Do not call a terminology service with a
raw patient question merely to discover whether a token is medical.

### Tasks

- **Generate the vocabulary:** add a standard-library-only update script and
  check in the generated index with source/year metadata.
- **Use the vocabulary:** merge generated terms and safe generated aliases into
  `VeterinaryWebQuery`, preserving the small curated set for species, language,
  modifiers, and privacy-sensitive routing.
- **Prove the path:** cover an NLM term such as leishmaniasis, Portuguese alias
  handling, trusted-source relevance filtering, and run shared lint/build checks.

### Acceptance criteria

- A new NLM disease term can be added by rerunning the update script rather
  than editing `VeterinaryWebQuery` term-by-term.
- Runtime public requests still contain only canonical vocabulary terms and
  never raw patient/owner text.
- A relevant trusted provider result produces a source card; unrelated results
  remain filtered.
