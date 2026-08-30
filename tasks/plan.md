# Implementation Plan: iOS Build Recovery and Regression Guard

## Overview

The repository audit found that the current commit fails to compile for the iOS simulator: `IosCloudLlmSettingsStore` no longer implements the full `CloudLlmSettingsStore` contract and also lost its `CloudLlmConfig` import. This batch restores the primary-platform build, adds platform-level coverage for cloud settings persistence, removes an expect/actual annotation mismatch warning, and strengthens the local commit guard so this class of regression is caught before a commit is created.

## Evidence and Priority

1. **P0 — iOS simulator build is broken.** `:shared:compileKotlinIosSimulatorArm64` reports missing `presetId`/`setPresetId` implementations and unresolved `CloudLlmConfig` references.
2. **P1 — No iOS persistence contract test covers every cloud setting.** Android, desktop, and common fakes implement the provider preset, but the iOS implementation regressed without a focused test.
3. **P1 — The pre-commit hook runs formatting/static analysis only.** It accepted a platform compile regression in an iOS-first project.
4. **P2 — Non-native `ObjCHidden` actuals omit the expect declaration's target/retention annotations.** Kotlin reports this as a compatibility warning.
5. **P1 — A wipe unit test mixes a fake database port with a real FTS repository.** The fake cannot clear the real metadata table, so the test is order/platform dependent and fails on iOS.
6. **P1 — The iOS PDF renderer casts Kotlin `String` to `NSString`.** Kotlin reports that the cast can never succeed; a native smoke test is needed to prove report generation does not crash.
7. **P2 — Native file/export calls and two sync assertions emit avoidable warnings.** Explicit interop opt-ins and nullable assertion results keep compiler output actionable.
8. **P0 — Two different golden suites declare `RagGoldenSetTest`.** `commonTest` is inherited by the Android host test compilation, so the real retrieval suite and orchestration suite collide and prevent tests from compiling.
9. **P2 — The Git-hook installer discards Gradle's configuration cache.** The task captures the project from `doLast`, adding noisy warnings and repeated configuration work to setup.

## Architecture Decisions

- Keep cloud-setting defaults and provider semantics in shared Kotlin; the iOS store remains a thin `NSUserDefaults`/Keychain adapter.
- Test the iOS adapter with an isolated `NSUserDefaults` suite and an in-memory `SecureStore`, avoiding the real Keychain and user preferences.
- Add the iOS simulator Kotlin compilation task to the pre-commit gate. The build is incremental, and the repository is explicitly iOS-first.
- Keep this batch focused on restoring correctness and preventing recurrence. Product features remain a separate vertical slice after the build is green.

## Dependency Graph

```text
CloudLlmSettingsStore contract
            │
            ├── IosCloudLlmSettingsStore implementation
            │             │
            │             └── iOS persistence regression test
            │
            └── iOS simulator compilation gate

ObjCHidden expect declaration
            │
            └── Android/Desktop actual annotation parity
```

## Task List

### Phase 1: Restore the Primary Platform

#### Task 1: Repair iOS cloud settings persistence

**Description:** Restore the missing shared config import and provider-preset accessors in the iOS settings adapter.

**Acceptance criteria:**

- The iOS store implements every `CloudLlmSettingsStore` member.
- Model/base-URL defaults still come from shared `CloudLlmConfig`.
- Provider selection survives a new store instance using the same defaults suite.

**Verification:**

- `./gradlew :shared:compileKotlinIosSimulatorArm64`
- Focused iOS settings-store test through `:shared:iosSimulatorArm64Test`

**Dependencies:** None

**Files likely touched:**

- `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/presentation/settings/IosCloudLlmSettingsStore.kt`
- `shared/src/iosTest/kotlin/com/github/rodrigotimoteo/animally/IosCloudLlmSettingsStoreTest.kt`

**Estimated scope:** Small

#### Task 2: Align expect/actual annotation metadata

**Description:** Add the expect declaration's target and retention metadata to Android and desktop no-op actual annotations.

**Acceptance criteria:**

- Android compilation no longer reports missing annotation metadata for `ObjCHidden`.
- ObjC hiding behaviour on iOS remains unchanged.

**Verification:**

- `./gradlew :shared:compileAndroidMain :shared:compileKotlinIosSimulatorArm64`

**Dependencies:** None

**Files likely touched:**

- `shared/src/androidMain/kotlin/com/github/rodrigotimoteo/animally/bridge/ObjCHidden.android.kt`
- `shared/src/desktopMain/kotlin/com/github/rodrigotimoteo/animally/bridge/ObjCHidden.desktop.kt`

**Estimated scope:** Small

### Checkpoint: Platform Compilation

- [x] iOS simulator Kotlin compilation succeeds.
- [x] Android shared compilation succeeds without the `ObjCHidden` warning.
- [x] The focused iOS settings test passes.

#### Task 3: Make wipe delegation coverage deterministic

**Description:** Keep the real-database wipe test as the FTS/data integration gate, and make the fake-port unit test assert delegation through a tracking search port instead of combining fake and real storage.

**Acceptance criteria:**

- The unit test verifies database wipe, audio deletion, and one search rebuild call.
- The test does not depend on SQLDelight state or platform test ordering.
- The existing full-database wipe test remains unchanged as the persistence gate.

**Verification:**

- `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`

**Dependencies:** None

**Files likely touched:**

- `shared/src/commonTest/kotlin/com/github/rodrigotimoteo/animally/domain/settings/WipeAllDataUseCaseTest.kt`

**Estimated scope:** Small

#### Task 4: Verify the iOS PDF renderer bridge

**Description:** Add a native smoke test that renders a minimal report and repair the Kotlin-to-Foundation string bridge if the generated PDF path crashes or returns invalid bytes.

**Acceptance criteria:**

- A minimal iOS report renders without a class-cast failure.
- The result is non-empty and begins with the PDF file signature.
- The impossible-cast compiler warning is removed.

**Verification:**

- Focused iOS PDF generator test through `:shared:iosSimulatorArm64Test`
- `./gradlew :shared:compileKotlinIosSimulatorArm64`

**Dependencies:** None

**Files likely touched:**

- `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/domain/export/pdf/PdfGenerator.ios.kt`
- `shared/src/iosTest/kotlin/com/github/rodrigotimoteo/animally/domain/export/pdf/IosPdfGeneratorTest.kt`

**Estimated scope:** Small

#### Task 5: Remove avoidable native/test warning noise

**Description:** Apply the required `BetaInteropApi` opt-in at the narrow file/class boundaries that use Foundation factories and retain non-null assertion results in sync tests.

**Acceptance criteria:**

- Native storage/export sources no longer emit missing `BetaInteropApi` opt-in warnings.
- Sync tests no longer use unnecessary non-null assertions.
- No warning is hidden with a broad compiler suppression.

**Verification:**

- `./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileTestKotlinIosSimulatorArm64`

**Dependencies:** Task 4

**Files likely touched:** Native storage/export files and `SyncViewModelTest.kt`

**Estimated scope:** Small, mechanical

#### Task 6: Give RAG golden suites distinct identities

**Description:** Rename the common orchestration golden suite so it can coexist with the Android-host real-database retrieval golden suite.

**Acceptance criteria:**

- Both golden suites compile into the Android host test binary.
- The common suite remains available to iOS tests.
- No tests or coverage are removed.

**Verification:**

- `./gradlew :shared:compileAndroidHostTest :shared:compileTestKotlinIosSimulatorArm64`
- Full shared test gate

**Dependencies:** None

**Files likely touched:**

- Common orchestration golden-set test filename/class only

**Estimated scope:** Extra small

### Phase 2: Prevent Recurrence

#### Task 7: Add iOS compilation to the local commit gate

**Description:** Extend the checked-in pre-commit hook to run the primary iOS simulator Kotlin compilation alongside Detekt and KtLint.

**Acceptance criteria:**

- A shared/iOS compile error rejects the commit.
- The hook keeps its existing lint/format failure guidance.
- The installed local hook is refreshed from the checked-in script.

**Verification:**

- `sh gradle/pre-commit.sh`
- `./gradlew installGitHooks`

**Dependencies:** Tasks 1, 4, 5, and 6

**Files likely touched:**

- `gradle/pre-commit.sh`

**Estimated scope:** Small

#### Task 8: Make hook installation cache-safe

**Description:** Model hook installation as a Gradle `Copy` task with declared inputs, output, and executable permissions.

**Acceptance criteria:**

- Hook installation succeeds without configuration-cache problems.
- A second invocation reuses the stored configuration cache and is up to date.

**Verification:**

- Run `./gradlew installGitHooks` twice and confirm cache storage followed by reuse.

**Dependencies:** Task 7

**Files likely touched:**

- `build.gradle.kts`

**Estimated scope:** Small

### Checkpoint: Complete

- [x] `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:ktlintCheck :shared:detekt`
- [x] iOS Xcode simulator build succeeds after the shared gate is green.
- [x] Diff review confirms no business logic moved into Swift.
- [x] All changes are committed together with the completed plan state.

## Ranked Product Backlog

These are promising follow-up slices, not part of this build-recovery batch:

1. **Internship Insights dashboard:** date-range caseload, procedure counts, reproduction outcomes, and thesis-ready CSV/PDF export.
2. **Needs-review inbox:** failed dictation extractions, incomplete records, overdue care, and unsourced assistant claims in one actionable queue.
3. **Record templates:** reusable consultation/treatment templates and quick-add favourites for repetitive field work.
4. **Backup health:** last successful backup, media coverage, CloudKit status, and a clear warning when the device is carrying unprotected records.
5. **Data-quality audit:** missing microchips, owner locations, breeding dates, due dates, and inconsistent patient links.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| iOS defaults tests mutate real app preferences | Medium | Use a dedicated suite and erase its persistent domain before/after each test. |
| Pre-commit becomes slower | Low | Use the incremental simulator compilation task and retain an explicit `--no-verify` escape hatch for emergencies. |
| Platform annotation fix changes ObjC export | Low | Change Android/desktop no-op actuals only; keep the iOS typealias untouched. |

## Open Questions

None for this batch. The user authorised a broad improvement pass; product-backlog items can be selected after the build-recovery checkpoint.
