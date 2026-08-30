# Implementation Plan: Internship Insights Dashboard

## Overview

Build an iOS-first, cross-platform insights feature that turns Animally's existing clinical and reproduction records into an auditable internship overview and research-ready export. Every count and chart is calculated deterministically in shared Kotlin from persisted records; Swift renders the native iOS experience but owns no metric logic. Every dashboard value can be opened to reveal the records behind it.

The first release deliberately reports facts such as activity counts, case mix, breeding events, active gestations, embryo collections, and ICSI totals. It must not claim conception, transfer-success, or foaling-success rates until the data model can link attempts to outcomes with an explicit denominator.

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

- Date presets: 7 days, 30 days, 90 days, current semester, custom, all time.
- Scope: all patients or one patient; owner/cohort filters are follow-ups.
- Optional record-type filter for drill-down.
- Comparison: previous period of equal length, disabled for all-time/custom ranges without a valid comparison.

## Metrics That Are Defensible Today

| Metric | Definition | Existing source |
| --- | --- | --- |
| Patients seen | Distinct patients with at least one active dated record in range | Existing patient-linked record tables |
| Recorded activities | Count of active dated records in range; never labelled “procedures” | Existing record tables |
| Active days | Distinct dates with at least one activity | Existing record dates |
| Activities per active day | Recorded activities divided by active days | Derived in shared use case |
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

- `InsightsFilter(from, to, patientId, recordTypes)`
- `InsightsSnapshot(overview, activitySeries, recordMix, reproduction, currentCare, dataIssues)`
- `OverviewMetrics(patientCount, activityCount, activeDayCount, averagePerActiveDay, comparison)`
- `MetricComparison(current, previous, absoluteDelta, percentageDelta?)`
- `ActivityPoint(periodStart, count)`
- `RecordTypeCount(type, count, share)`
- `ReproductionMetrics(eventCounts, embryoCollections, embryosCollected, icsiSessions, folliclesRecovered)`
- `CurrentGestationItem(patientId, patientName, gestationDay, dueDate, daysUntilDue, status)`
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

## Task List

### Phase 1: Trustworthy Foundation

#### Task 1: Canonicalise reproduction event types

**Description:** Add a shared event taxonomy that recognises current Android/iOS legacy values and supplies one stable persisted wire value and one display label.

**Acceptance criteria:**

- Both `PregnancyCheck` and `Pregnancy Check` map to the same category.
- New Android and iOS saves use shared wire values.
- Unknown historical values remain visible as `Other`, not discarded.

**Verification:** Unit tests for every legacy/canonical value; Android host and iOS simulator compilation.

**Dependencies:** None

**Files likely touched:** Shared taxonomy/model, reproduction form state/view model, platform picker adapters, tests.

**Estimated scope:** Medium, split platform picker wiring from the shared taxonomy if it exceeds five files.

#### Task 2: Define the insights contract

**Description:** Add the immutable filter, snapshot, metric, comparison, drill-down, and repository contracts in shared Kotlin.

**Acceptance criteria:**

- Models distinguish period metrics from current-state care items.
- Empty and zero-denominator states are representable without fake percentages.
- Models export cleanly to Swift.

**Verification:** Common model tests plus iOS simulator test compilation.

**Dependencies:** Task 1

**Files likely touched:** `domain/insights/model/*`, `domain/insights/IInsightsRepository.kt`.

**Estimated scope:** Small

#### Task 3: Implement overview and activity facts

**Description:** Add SQLDelight aggregate queries and a repository implementation for patients seen, activity count, active days, activity series, and record mix.

**Acceptance criteria:**

- Only active rows inside the inclusive range are counted.
- Patient-scoped and global results use identical definitions.
- A fixture containing one record of each type produces exact expected totals without duplicate joins.

**Verification:** Android-host SQLDelight integration tests and query performance check on a large fixture.

**Dependencies:** Task 2

**Files likely touched:** `Insights.sq`, repository implementation, mapper, integration test.

**Estimated scope:** Medium

### Checkpoint: Foundation

- Reproduction taxonomy tests pass.
- Shared insights facts are deterministic across Android host and iOS simulator.
- No LLM or Swift code participates in calculations.

### Phase 2: First Useful Dashboard

#### Task 4: Build the overview use case and shared state

**Description:** Calculate comparison periods, safe percentages, chart granularity, empty states, and drill-down filters in a shared use case and ViewModel.

**Acceptance criteria:**

- Date and patient filter changes reload atomically without stale results.
- Zero previous values produce an unavailable percentage rather than infinity.
- Loading, empty, content, and error states are explicit.

**Verification:** ViewModel coroutine tests with fake repositories and fixed clock/date provider.

**Dependencies:** Task 3

**Files likely touched:** Use case, ViewModel, UI state, DI registration, tests.

**Estimated scope:** Medium

#### Task 5: Render the iOS overview and case mix

**Description:** Add a SwiftUI dashboard using native Swift Charts and the existing theme/accent environment, with Timeline and Patient Detail entry points.

**Acceptance criteria:**

- Summary cards, activity chart, record mix, filters, loading, and empty states work in light/dark/system themes.
- Charts are accessible without relying on colour alone.
- Tapping a card or chart category opens a filtered source-record list.

**Verification:** Xcode build, simulator screenshots at small/large Dynamic Type, VoiceOver labels, and mock-fixture UI walkthrough.

**Dependencies:** Task 4

**Files likely touched:** Insights SwiftUI view/wrapper, navigation destination, Timeline/Patient Detail entry points.

**Estimated scope:** Medium; split navigation from rendering if needed.

### Checkpoint: MVP Overview

- A user can choose a range, understand workload/case mix, and inspect every source record.
- Counts match fixture calculations exactly.
- Existing five-tab navigation remains uncluttered.

### Phase 3: Reproduction and Operational Value

#### Task 6: Add reproduction-period metrics

**Description:** Aggregate canonical reproduction events, embryo collections/counts, ICSI sessions/follicles, and ultrasound activity.

**Acceptance criteria:**

- Metrics use structured fields and show explicit denominators.
- Unknown event types appear in an `Other` bucket.
- No conception or transfer-success rate is presented.

**Verification:** Golden fixture tests covering legacy event labels, zero counts, multiple patients, and soft-deleted rows.

**Dependencies:** Tasks 1 and 3

**Files likely touched:** Insights SQL/repository, reproduction metrics use case, tests, shared state.

**Estimated scope:** Medium

#### Task 7: Add current gestation and due-soon cards

**Description:** Present active gestations as a current snapshot with gestation day, expected due date, and due-soon grouping independent of the historical range.

**Acceptance criteria:**

- Gestation day is calculated from breeding date and the injected current date.
- Resolved/inactive gestations are excluded.
- Each mare links directly to her gestation record.

**Verification:** Boundary tests for today, overdue, leap dates, resolved statuses, and 30/60/90-day groups.

**Dependencies:** Task 4

**Files likely touched:** Shared use case/state, ViewModel tests, iOS section view.

**Estimated scope:** Medium

### Phase 4: Thesis-Ready Output

#### Task 8: Export an auditable analysis bundle

**Description:** Export summary, activity-series, record-mix, reproduction, data-issues, and data-dictionary CSV files using the active filter. Add optional stable pseudonymous patient identifiers.

**Acceptance criteria:**

- Export contains metric definitions, range, scope, generation time, and app/schema version.
- Pseudonymised mode excludes patient names and all owner contact/location data.
- Exported totals exactly match dashboard totals.

**Verification:** Golden CSV tests, privacy-field assertion, and round-trip parsing with quoted/newline-containing values.

**Dependencies:** Tasks 4, 6, and 7

**Files likely touched:** Export use case, CSV formatter reuse/extension, pseudonym service, tests, iOS share action.

**Estimated scope:** Medium; PDF summary can be a separate follow-up.

#### Task 9: Add research-readiness checks

**Description:** Show explicit counts for records that weaken analysis: unknown event categories, missing vet names, unlinked owners, free-text recipients, and incomplete structured ultrasound fields.

**Acceptance criteria:**

- Every issue count opens the affected records.
- The UI explains why the field matters without claiming records are clinically wrong.
- No opaque aggregate quality score is displayed.

**Verification:** Rule unit tests and drill-down fixture tests.

**Dependencies:** Tasks 4 and 6

**Files likely touched:** Data-quality rules/use case, shared state, iOS section, tests.

**Estimated scope:** Medium

### Checkpoint: Complete MVP+

- Full shared tests, Detekt, KtLint, and Xcode simulator build pass.
- Dashboard and export agree on every fixture total.
- Source drill-down works for every metric.
- Privacy review confirms no owner-identifying data in pseudonymised exports.
- Implementation is committed with a clean worktree.

## High-Value Follow-Ups

1. **Saved thesis cohorts:** reusable inclusion/exclusion filters with a frozen cohort snapshot and change log.
2. **Reproduction cycle linkage:** explicit breeding attempts, pregnancy-check outcomes, recipient links, and foaling outcomes to unlock honest success rates.
3. **Internship sessions:** date, hours, location, supervisor, role, procedures observed/performed, competencies, and reflection; dashboard can then track official placement hours rather than infer them from records.
4. **Competency portfolio:** progress by procedure/category with evidence links and supervisor sign-off.
5. **Reproduction pipeline:** mares grouped as monitoring, ready to breed, bred/awaiting check, pregnant, due soon, or follow-up needed.
6. **Study workspace:** variable selector, cohort comparison, long/wide export, missingness table, and reproducible analysis snapshot.
7. **Geographic caseload:** owner/stable map with privacy-preserving aggregation and travel-day summaries.
8. **Assistant explanation mode:** the LLM may explain a frozen `InsightsSnapshot`, but every statement must cite a dashboard metric or underlying record and it cannot recompute values.
9. **Supervisor report:** monthly PDF containing hours, case mix, procedures, reflections, and linked evidence for review/sign-off.
10. **Follow-up inbox:** due gestation checks, incomplete records, failed dictation extraction, expiring preventive care, and dashboard data-quality issues in one queue.

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

## Recommended Starting Slice

Implement Tasks 1–5 first. That produces a genuinely useful, auditable workload and case-mix dashboard without schema migration or questionable outcome calculations. Then add Tasks 6–9 once the metric contract and interaction pattern are proven on Daniela's real workflow.

## Open Questions Before Implementation

- What date defines Daniela's semester/internship reporting periods?
- Does her degree require documented placement hours, competency categories, or supervisor signatures?
- Should patient-scoped Insights be available for every horse or only reproduction cases?
- For thesis exports, should pseudonymous IDs remain stable across exports or be unique per study?
