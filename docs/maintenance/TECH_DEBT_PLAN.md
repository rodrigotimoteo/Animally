# Animally Technical Debt Plan

Status: implementation baseline — deterministic P0/P1 slices landed; policy/live-environment items remain

Last updated: 2026-09-05

Scope: `:shared`, `:androidApp`, `iosApp`, build/verification tooling, and maintained documentation

> This file began as a static planning baseline. It now also records the authorized implementation slices and their verification evidence. The historical findings remain below for traceability; the execution record is authoritative for current status.

## Executive diagnosis

Animally's primary maintenance risk is not ordinary CRUD complexity. It is inconsistent state across the SQLDelight database, soft deletes, CloudKit/generic sync, FTS search and RAG, backup JSON/raw database copies, attachment files, scheduled notifications, and native UI bridges. These subsystems maintain overlapping record knowledge through separate manual maps and side effects. A local write may therefore be correct while a backup, remote device, search result, assistant answer, notification, or wipe operation remains stale or incomplete.

The implementation pass closed the deterministic portions of that path: restore now rejects malformed JSON rows before mutation, resets stale local sync/index state, local sync carries ownerless records and tombstones, CloudKit cursor retries are bounded, remote changes heal search, exports cover the repaired record families, reminder cleanup is persisted/cancellable, and the Android/iOS settings operations no longer block their UI callers. The remaining safety decisions are portable media and raw snapshot semantics, post-commit restore outcomes, cloud dataset/wipe policy, and backup privacy.

The release-confidence gap is now explicit rather than hidden in local convention: lint, Detekt, Kover, Android packaging, shared Android/desktop tests, native iOS compilation, the iOS host build, and script contracts pass. Live simulator/UI, Android runtime permission, CloudKit account/callback, migration-fixture, CI, and capability-matrix evidence remain open. Broad architectural cleanup should wait until those contracts are measured.

The repository has already completed much of the historical cleanup described in `.slim/deepwork/integrity-simplify.md` through the shared-kernel, LLM, dependency-inversion, presentation, and quality commits. This plan does not reopen that completed work. It records residual defects and the smallest dependency-ordered path to a trustworthy maintenance baseline.

## Execution record — 2026-09-05

The implementation pass reduced the highest-risk local and deterministic platform debt without adding production dependencies or changing the product's unresolved cloud/privacy policy. The following status table supersedes the original “planned disposition” column in the baseline map.

| Debt | Current status | Evidence / scope boundary |
|---|---|---|
| TD-02 | Completed | Backup schema is v2; v1 is normalized on read and newer unsupported versions are rejected. |
| TD-03 | Completed for JSON rows | Restore preflight validates positive/unique IDs, patient/owner/ultrasound references, dates, counts, and reminder link pairs before mutation. Media manifest/hash validation remains deferred with portable packaging. |
| TD-05 | Completed locally | Restore clears domain rows, sync metadata/state, and search markers before rebuilding. Cloud rejoin/new-dataset policy remains a product decision. |
| TD-07 / TD-24 | Completed for local reminder lifecycle | Custom reminder deletion and full wipe cancel notifications; enablement persists on Android/iOS/desktop and disabling/permission loss cancels stale requests. |
| TD-08 / TD-09 / TD-11 | Completed in deterministic sync paths | Ownerless patients and explicit owner unlinking are represented; tombstones use raw rows and inactive changed-since queries; reproduction/ultrasound fields and Follicle are covered by handler/registry/order contracts. |
| TD-10 | Completed | CloudKit export cursor pins before failed timestamps and safely retries equal-timestamp rows. |
| TD-12 / TD-13 / TD-19 | Completed in deterministic/local startup paths | Remote apply marks search dirty and heals it; FTS count/orphan/mismatch checks are part of health; Android startup launches the same healing pass as iOS. |
| TD-14 | Completed | CSV and PDF export include Custom Reminder, Embryo Transfer, ICSI, and Follicle sections with focused fixture coverage. |
| TD-15 / TD-16 / TD-18 / TD-20 | Completed in shared/Android code | Search retains owner identity and typed routing, date editing preserves partial input, sharing grants Activity launch permission, and Settings scrolls on compact layouts. |
| TD-17 | Implemented; runtime prompt unverified | Android now bridges permission requests through the Activity result launcher; a live Android 13+ prompt still needs device evidence. |
| TD-21 | Partially completed | Settings export/restore/PDF/wipe work off the UI dispatcher with duplicate-submit guards and status reporting. Restore still needs a typed committed-with-recovery outcome. |
| TD-22 | Deterministic portion completed | CloudKit starts idempotently from the production sync entrypoint and callback waits time out; account/entitlement/live callback behavior remains unverified. |
| TD-01 / TD-04 / TD-06 / TD-23 / TD-35 | Deferred or partial by policy | Portable media, WAL-safe raw snapshots, complete per-record media cleanup, cloud wipe/rejoin, and sensitive assistant backup handling require explicit product/storage decisions. Wipe now enumerates active and inactive audio/images and reports residual deletion failures. |
| TD-25 / TD-26 | Completed | Query ownership is derived from the real `.sq` tree with explicit support-schema ownership; negative/positive fixtures and simulator helper/docs are maintained. |
| TD-27 | Reduced, runtime isolation pending | The 75-second exploratory `FolliclePause` UI test was removed; isolated UI execution remains blocked by CoreSimulatorService. |
| TD-28 / TD-29 / TD-30 / TD-32 / TD-33 | Deferred | CI, migration fixtures, domain/data extraction, host capability decisions, and assistant cancellation remain separate follow-up work. |
| TD-31 / TD-34 | Partially completed | Reproductive/export/search/sync parity is covered for the repaired slices; full descriptor consolidation, generated-vocabulary provenance, stale architecture counts, and desktop Detekt scope remain. |

No new production dependency was introduced. The only static-analysis suppression added is local to the explicit nine-family reproductive export coordinator and documents why those dependencies stay visible.

### Verification completed

- `:shared:testAndroidHostTest` — full suite passed after the final common-code changes.
- `:shared:desktopTest` — full suite passed after the final common-code changes.
- `:shared:koverVerify` — configured 57% merged JVM line-coverage floor passed.
- `:shared:ktlintCheck` and `:shared:detekt` — passed.
- `:shared:compileKotlinIosSimulatorArm64` and `:shared:compileTestKotlinIosSimulatorArm64` — passed.
- `:androidApp:compileDebugKotlin`, `:androidApp:lintDebug`, and `:androidApp:assembleDebug` — passed.
- `bash -n`/`sh -n`, `scripts/check-queries-module.sh`, its positive/negative fixture suite, and `scripts/sim-e2e.sh help` — passed.
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO build` — passed, including the Swift Settings bridge after error handling was added.

The simulator UI lane was attempted separately. `xcrun simctl list devices available` reported a refused/invalid CoreSimulatorService connection, so no UI pass, screenshot, or device-prompt result is claimed. The generic host build is compile evidence only.

## Scope, confidence, and current baseline

- **Confirmed bug** means a deterministic trigger-to-impact path is visible in source, without relying on a speculative external failure.
- **High-confidence bug** means the source path is complete but one environmental or product-policy premise still needs a focused runtime or contract test.
- **Debt** means a material maintainability or release-confidence weakness without a demonstrated user-visible failure.
- **Audit prompt** means evidence is insufficient to schedule a fix; it remains intentionally deferred until reproduced or measured.

The working tree already contained user-owned changes when this plan was prepared:

- `.codex/config.toml`
- `AGENTS.md`
- `shared/src/androidHostTest/kotlin/com/github/rodrigotimoteo/animally/data/search/RecordTypeIndexingTest.kt`
- `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/data/search/SearchIndexerRegistry.kt`
- `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/medication/usecase/SaveMedicationUseCase.kt`
- `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/search/ISearchRepository.kt`
- `shared/src/commonTest/kotlin/com/github/rodrigotimoteo/animally/domain/medication/usecase/SaveMedicationUseCaseTest.kt`

Those changes addressed missing medication `startDate` metadata and bumped the search index version to 23. They were preserved, integrated with the surrounding parity work, and validated by the focused and full shared test/quality matrix above; unrelated user-owned `.codex` and instruction changes were left untouched.

## Complete material pain-point map — original baseline

| ID | Priority | Classification | Material pain point | Impact | Planned disposition |
|---|---:|---|---|---|---|
| TD-01 | P0 | Confirmed bug | Shared backup contains path strings but not attachment/audio bytes; Settings shares only JSON | Cross-install/device restore produces dangling clinical image and dictation paths | LUNA 2.4-2.6 |
| TD-02 | P0 | Confirmed bug | Backup schema remains version 1 while new defaulted fields are added and unknown keys are ignored | An older app accepts a newer backup and silently drops newer record families on restore | LUNA 2.2 |
| TD-03 | P0 | High-confidence bug | Restore validates size/count/version but not IDs, uniqueness, or parent relationships | Crafted/corrupt backups can commit orphaned or contradictory domain rows | LUNA 2.1-2.3 |
| TD-04 | P0 | High-confidence bug | JSON tables are read sequentially and the live DB is copied separately | Concurrent writes can create internally inconsistent JSON and a raw copy from a different moment; WAL safety is unproven | LUNA 2.4 |
| TD-05 | P0 | Confirmed bug | JSON restore drops `serverId` values but retains sync metadata/cursors | Restored rows can duplicate remote records or never re-export | LUNA 1.2, 2.3, 4.1 |
| TD-06 | P0 | Confirmed bug | Wipe/delete ignores failed audio deletion and never enumerates clinical images | UI can report permanent erasure while private media remains on disk | LUNA 2.7 |
| TD-07 | P0 | Confirmed bug | Deleting custom reminders does not cancel scheduled OS notifications | A deleted reminder can still notify the user; wipe has the same gap | LUNA 2.8 |
| TD-08 | P0 | Confirmed bug | Valid ownerless patients serialize a null optional parent, which sync interprets as unresolved | Ownerless patients and their children never export; unlinking an owner is unrepresentable | LUNA 3.2 |
| TD-09 | P0 | Confirmed bug | Active-only queries/builders prevent local tombstones from reaching sync | Deleted records remain active remotely or are resurrected | LUNA 3.3 |
| TD-10 | P0 | Confirmed bug | CloudKit advances its export cursor past an older failed row | A failed change can be skipped permanently | LUNA 3.4 |
| TD-11 | P0 | Confirmed bug | Sync payloads omit structured reproduction/ultrasound fields and omit Follicle as an entity | Clinically material data is absent remotely or overwritten with nulls | LUNA 3.5 |
| TD-12 | P0 | Confirmed bug | Sync applies rows through plain repositories without maintaining/invalidation of FTS | Search and assistant grounding remain stale after remote changes | LUNA 4.2 |
| TD-13 | P1 | Confirmed bug | Search health checks metadata rows/version, not the FTS half of the projection | An empty/corrupt FTS table can be accepted indefinitely | LUNA 4.3 |
| TD-14 | P1 | Confirmed bug | CSV aggregation/export omits Custom Reminder, Embryo Transfer, ICSI, and Follicle detail | An export documented as every patient record is incomplete | LUNA 4.4 |
| TD-15 | P1 | Confirmed bug | Android owner search stores an owner ID as `patientId` and always opens patient detail | Tapping an owner result opens the wrong entity; ID collisions can merge groups | LUNA 5.2 |
| TD-16 | P1 | Confirmed bug | Android search parses a complete `LocalDate` on every keystroke | Normal incremental `yyyy-MM-dd` entry clears itself | LUNA 5.3 |
| TD-17 | P1 | Confirmed bug | Android notification permission cannot launch a runtime request | Android 13+ reminder enablement silently returns to disabled | LUNA 5.5 |
| TD-18 | P1 | Confirmed bug | Android sharing uses Application context without `FLAG_ACTIVITY_NEW_TASK` | CSV, backup, and PDF sharing throws when Settings becomes reachable | LUNA 5.4 |
| TD-19 | P1 | Confirmed bug | Android never invokes startup `reindexIfNeeded` | Existing/install-upgraded Android data may remain absent or stale in search | LUNA 4.3, 5.1 |
| TD-20 | P1 | Confirmed bug | Android Settings is a non-scrollable full-height column | Lower sections are clipped on compact screens if/when the route is exposed | LUNA 5.1 |
| TD-21 | P1 | High-confidence bug | Restore and export run synchronously from settings state; post-commit cleanup/reindex is reported as total restore failure | Large backups can freeze UI, and retry follows an ambiguous committed result | LUNA 2.3, 5.6 |
| TD-22 | P1 | High-confidence bug | CloudKit event waits have no timeout and no production `start()` call was found | Enabling the dormant path or losing a callback can hold sync forever | LUNA 3.1, 3.6 |
| TD-23 | P1 | High-confidence bug | Wipe clears local rows but not cloud state/tombstones | A later cloud fetch can repopulate data the user believed erased | LUNA 1.2, 2.7, 3.3 |
| TD-24 | P1 | Confirmed bug | Reminder enablement is only an in-memory StateFlow | The preference resets after ViewModel/app recreation | LUNA 2.8 |
| TD-25 | P1 | Confirmed tooling defect | `check-queries-module.sh` hard-codes three direct-owned query sets, but there are four | The integrity check currently false-fails and is not a reliable gate | LUNA 0.2 |
| TD-26 | P1 | Confirmed tooling defect | Simulator script/docs use inconsistent commands/env names and hard-coded device/build paths | Documented E2E steps are not reproducible and can report a false boot success | LUNA 0.3 |
| TD-27 | P1 | Confirmed test defect | iOS UI tests depend on pre-existing mutable data and include `sleep(75)` in a test target | Clean runs fail or mutate shared state; feedback is unnecessarily slow | LUNA 0.4 |
| TD-28 | P1 | Debt | No tracked CI; pre-commit and README gates omit important hosts/tests | Release confidence depends on local convention and historical artifacts | LUNA 7.1-7.3 |
| TD-29 | P1 | Debt | No fixture-based upgrade tests across 17 SQLDelight migrations; recovery providers are no-op | Migration compatibility and failed-upgrade recovery are unproven | LUNA 7.2 |
| TD-30 | P2 | Debt | Backup/timeline domain packages import generated database/data-layer types | Domain tests and changes are coupled to persistence implementation | LUNA 6.2 |
| TD-31 | P2 | Debt | Record capabilities are duplicated across save use cases, FTS registry, CSV, timeline, sync enums/handlers/SQL, and Swift metadata | New record types/fields drift between projections, as current medication and reproductive gaps show | LUNA 4.5, 6.1 |
| TD-32 | P2 | Debt | Android's product shell exposes only a subset of registered/shared features; iOS and Android settings/sync claims diverge | Latent Android defects go unexercised and product capability is ambiguous | LUNA 1.1, 5.1 |
| TD-33 | P2 | Debt | Assistant work has no user cancellation; `StoreAwait` has a single-resume/cancellation audit risk | Slow work can lock interaction; bridge races remain possible | LUNA 5.7 |
| TD-34 | P2 | Debt | ADR/status/architecture counts, coverage baseline, Detekt scope/baseline, and generated-vocabulary provenance are stale or weak | Maintainers cannot reliably infer current support and quality guarantees | LUNA 7.3-7.5 |
| TD-35 | P2 | Privacy decision | Assistant Q/A/transcripts are backed up to user-visible storage despite migration comments calling them device-local sensitive data | Backup behavior may exceed the intended privacy boundary | LUNA 1.3 |

## Historical evidence: pre-implementation baseline

The findings in this section describe the source state before the execution record above. They are retained to show why each slice was selected; use the current-status table for what remains true now.

### A. Backup, restore, erase, and export

**TD-01 — backup media is not portable (confirmed).** `README.md:44-50` and `docs/adr/0014-dual-backup-formats.md:6-18` describe recovery/transfer and raw database plus attachments. `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/backup/BackupPayload.kt:63-101` contains database DTO lists and explicitly says raw dictation audio is not embedded. `BackupDtos.kt:14-34`, `BackupDtosClinical.kt:74-87`, `BackupDtosRepro.kt:39-63`, and `BackupDtosAssistant.kt:29-37` preserve only `photoUri`, `imageUris`, and `audioPath`. `ExportBackupUseCase.kt:45-51` produces JSON and a DB copy, but `presentation/settings/SettingsViewModel.kt:149-155` shares only `backupPath`. Restore writes those original paths back (`BackupRestoreBase.kt:71-79,105-127`, `BackupRestoreClinical.kt:64-78`, `BackupRestoreRepro.kt:25-49`) without importing bytes. Trigger: restore in a different app container; impact: records refer to missing media.

**TD-02 — forward backup compatibility can silently lose data (confirmed).** `BackupPayload.kt:17` fixes `BACKUP_SCHEMA_VERSION` at 1, while later fields at `:93-101` have been added with defaults. `BackupSerializer.kt:16-20` enables `ignoreUnknownKeys`; `:31-42` checks only equality with version 1. An older binary therefore accepts a newer version-1 payload, ignores new record collections, and then destructively replaces the database without them.

**TD-03 — restore lacks semantic/referential preflight (high confidence).** `BackupSerializer.kt:31-42,126-141` validates file size, schema number, and item counts. `RestoreBackupUseCase.kt:32-67` then clears and inserts supplied IDs. The schema deliberately has no database foreign-key enforcement; see `docs/adr/0010-app-layer-fk-enforcement.md:6-19`, `shared/src/commonMain/sqldelight/com/github/rodrigotimoteo/animally/data/migrations/1.sqm:17-32,128-149`, and `shared/src/commonMain/sqldelight/com/github/rodrigotimoteo/animally/data/follicle/Follicle.sq:1-40`. A same-version payload with missing parents or duplicate identities can therefore commit invalid relationships.

**TD-04 — backup artifacts are not one snapshot (high confidence).** `ExportBackupUseCase.kt:45-83` reads tables sequentially without a surrounding snapshot transaction, writes JSON, and then copies the live DB. `shared/src/androidMain/kotlin/com/github/rodrigotimoteo/animally/data/backup/BackupStorage.android.kt:20-24` and `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/data/backup/BackupStorage.ios.kt:36-48` directly copy the open database file. Concurrent save/sync can split parent and child reads or make JSON/raw outputs represent different moments; journal/WAL correctness must be proven before choosing the raw-copy implementation.

**TD-05 — restore loses cloud identity while preserving cloud position (confirmed).** Backup DTOs omit `serverId` (`BackupDtos.kt:14-51`, `BackupDtosClinical.kt:37-51`), and restore insert paths omit it (for example `shared/src/commonMain/sqldelight/com/github/rodrigotimoteo/animally/data/medication/Medication.sq:46-48`). `BackupRestoreBase.kt:27-53` clears domain tables but not `SyncMetadata`, `SyncState`, or `SearchIndexState`; `CloudKitSyncSettings.kt:30-42` persists cursors. Pull matches by server ID (`MedicationSyncHandler.kt:86-117`). After restore, an unchanged remote row can be inserted as a duplicate while a restored local row may sit behind an old export cursor.

**TD-06 — erase/delete can leave private files (confirmed).** `DatabaseWipePortImpl.kt:20-56` returns dictation paths but not Imaging/Ultrasound `imageUris`. `WipeAllDataUseCase.kt:33-40` deletes only returned audio and ignores exceptions and a Boolean `false`; its success contract at `:9-15` is stronger than its behavior. `DeleteDictationCaptureUseCase.kt:15-23` likewise removes the row before best-effort file deletion. Both platform stores return `false` for failed deletion (`FileStorage.android.kt:17-25`, `FileStorage.ios.kt:53-68`). The UI can announce erasure while clinical media remains.

**TD-07/TD-24 — reminder lifecycle is incomplete (confirmed).** `NotificationScheduler.kt:21-39` has schedule operations only. `SaveCustomReminderUseCase.kt:39-64` schedules a stable notification, while `DeleteCustomReminderUseCase.kt:10-36` documents cancellation as a no-op and soft-deletes the record. `ReminderSettingsViewModel.kt:36-70,136-143` stores enablement only in memory. Delete, disable, wipe, or restart can therefore disagree with scheduled OS state.

**TD-14 — CSV is incomplete (confirmed).** `ExportRecords.kt:22-64` says every per-patient record, but `ExportCsvUseCase.kt:52-74`, `ExportReproductiveRecordsUseCase.kt:11-33`, and `CsvExporter.kt:25-85` omit Custom Reminder, Embryo Transfer, ICSI, and Follicle detail even though these appear in `BackupPayload.kt:93-97` and `iosApp/iosApp/Patients/Tabs/ReproductionTabView.swift:149-179`.

### B. Sync, search, and assistant grounding

**TD-08 — ownerless patients cannot sync (confirmed).** `PatientSyncHandler.kt:57-63,92` emits `ownerId -> null` for the valid nullable relationship. `SyncEngineImpl.kt:140-146` and `CloudKitSyncEngineImpl.kt:281-293` treat every null parent as unresolved and defer the row. `PatientSyncHandler.kt:183` also falls back to the old owner during a remote unlink. An ownerless patient never gains remote identity, so all patient children remain blocked.

**TD-09 — tombstones do not reliably export (confirmed).** `SyncEngineImpl.kt:102-108` discards inactive rows. CloudKit attempts tombstones at `CloudKitSyncEngineImpl.kt:296-305`, but handlers build through active-only repository reads, for example `MedicationSyncHandler.kt:48-50` and `shared/src/commonMain/sqldelight/com/github/rodrigotimoteo/animally/data/medication/Medication.sq:25-32`. Embryo Transfer and ICSI changed-since queries explicitly filter `isActive = 1` (`EmbryoTransfer.sq:51-52`, `Icsi.sq:50-51`). A locally deleted, previously synced row remains active elsewhere.

**TD-10 — a failed older export can be skipped permanently (confirmed).** `CloudKitSyncEngineImpl.kt:313-357` removes failures from `pending` but advances a maximum success timestamp; `:333` persists it. Changed-since SQL uses strict `updatedAt > cursor`, for example `Medication.sq:54-55`. If `t1` fails and `t2` succeeds, the next query starts after `t2` and never retries `t1`.

**TD-11 — reproductive sync drops data (confirmed).** `ReproductionEvent.kt:28-30` defines `initialExamFindings`, `stallionName`, and `breedingType`, but `ReproductionSyncHandler.kt:23-30,52-59,94-105,123-135` omits them. `Ultrasound.kt:33-40` has eight migration-7 fields absent from `UltrasoundSyncHandler.kt:23-33,55-65,99-114,132-147`. Follicles exist in `Follicle.sq:1-40` and `BackupDtosRepro.kt:65-78` but not `SyncEntityType.kt:16-38`, its registry, tracker, or order.

**TD-12 — remote changes bypass search/RAG maintenance (confirmed).** `SyncEngineImpl.kt:148-157` and `CloudKitSyncEngineImpl.kt:377-390` apply handlers directly. Handlers use plain repositories (for example `MedicationSyncHandler.kt:99-148`) without `ISearchRepository`. Startup healing can fast-path at `SearchRepositoryImpl.kt:102-109`; RAG consumes FTS evidence in `llm/rag/RagRetriever.kt:72-165`. A pulled change can therefore be visible in a list but absent or stale in search and assistant answers indefinitely.

**TD-13/TD-19 — the index health gate is incomplete and Android does not run it (confirmed).** `SearchFts.sq:30-45` stores metadata and FTS in distinct tables. `SearchRepositoryImpl.kt:102-109` counts metadata and version but does not verify FTS. `IosAppBridge.kt:36-42` invokes healing; `Initialize.android.kt:23-43` only starts dependency injection, with no Android production call to `reindexIfNeeded`. Existing `SearchIndexGateTest.kt:75-90` wipes FTS only after forcing an old version, so the current-version/empty-FTS case is uncovered.

**Baseline search bug — resolved.** The preserved user-owned patch in `SaveMedicationUseCase.kt`, `SearchIndexerRegistry.kt`, and `ISearchRepository.kt` adds medication `startDate` and bumps index version 23; `RecordTypeIndexingTest.kt` covers the regression. The focused and full shared matrix now verifies the fix.

### C. Platform/UI behavior

**TD-15 — Android owner search navigates as a patient (confirmed).** `data/search/mapper/SearchToDomainMapper.kt:59-77` stores `ownerId` in `SearchResult.patientId`. `presentation/search/view/SearchScreen.kt:213-230` groups by that value and invokes `onResultClick(patientId)` for every entity. `SearchViewModel.kt:83-86` always opens `Route.PatientDetail`. iOS correctly branches by entity at `iosApp/iosApp/Search/SearchView.swift:96-124`.

**TD-16 — Android date filtering cannot be typed incrementally (confirmed).** `SearchScreen.kt:171-184` renders from `LocalDate?` and parses the whole value on each keystroke. The first partial input fails parsing, stores null, and recomposes as an empty field.

**TD-17 — Android notification permission request is a no-op (confirmed).** `NotificationPermission.android.kt:18-22,33-36` can only recheck permission because it holds no Activity launcher. `ReminderSettingsViewModel.kt:65-85` treats `request()` as a real request. Android 13+ therefore never shows the runtime dialog from this path.

**TD-18 — Android share uses the wrong context contract (confirmed).** `AnimallyApplication.kt:6-10` and `Initialize.android.kt:21-27` install Application context. `ShareLauncher.android.kt:27-43` calls `startActivity` without `FLAG_ACTIVITY_NEW_TASK`; exports call it from `SettingsViewModel.kt:140-155,204-218`. The path throws when invoked from Application context.

**TD-20 — Android Settings will clip content (confirmed).** `SettingsScreen.kt:71-89` puts eight sections in a non-scrollable full-height `Column`; lower sync/reminder/danger content extends beyond compact-height viewports.

**TD-21 — restore completion is ambiguous and can block UI (high confidence).** `RestoreBackupUseCase.kt:35-67` commits database replacement, then `:68-70` performs audio cleanup/reindex outside that transaction. `SettingsViewModel.kt:161-172` converts any later exception into “Restore failed.” The Swift store is `@MainActor` (`iosApp/iosApp/Settings/SettingsViewModel.swift:4-5,64-68`), while decoding and up to 100,000 inserts are synchronous (`BackupPayload.kt:20-29`). A late failure can invite a retry after commit, and a large valid file can monopolize the UI thread.

**TD-22 — the dormant CloudKit path can wait forever (high confidence).** `CloudKitSyncEngineImpl.kt:55-56,313-333,360-374` waits on an event channel without timeout. No production call to its `start()` was found; `IosAppBridge.kt:36-42` starts Koin and search only. `CloudKitSyncSettings.kt:7-8,24-28` defaults disabled and has no discovered production UI writer; native `SettingsView.swift:20-28` has no sync section. Meanwhile Android exposes Cloud Sync in `SettingsScreen.kt:324-370` against intentionally disabled `KtorSyncApi.kt:12-30`. Product claims and executable capability are inconsistent.

### D. Verification defects

**TD-25 — SQLDelight integrity script false-fails (confirmed).** `scripts/check-queries-module.sh:4,15-19,35-40` expects three intentionally unbound query sets. `QueriesModule.kt:40-44` documents the old three, but `SqlDelightInsightsRepository.kt:49-50,58,68,92,98,112,120` and `SqlDelightInsightsDataIssueReader.kt:39` directly own `database.insightsQueries`, making Insights the fourth. The count-only check is also unwired and cannot detect duplicate/wrong bindings.

**TD-26 — simulator workflow is non-reproducible (confirmed).** `scripts/README-e2e.md:31` documents nonexistent `shot-after-tap`; `sim-e2e.sh:125,129` implements `screenshot`. The README uses `WINDOW_POS` at `:61`, while the script reads `WIN_X/WIN_Y` at `:12-13`; the script positions a configured coordinate at `:83-84`, not the documented live window position. `sim-e2e.sh:6,8` hard-codes a UDID and DerivedData hash, `:99-104` builds without matching `-derivedDataPath`, and `:43-45` suppresses boot errors before printing success.

**TD-27 — iOS UI tests are stateful and paused (confirmed).** `iosApp/iosUITests/TestHelpers.swift:7-24,47-51` neither seeds nor resets and selects the first pre-existing patient/owner. `OwnerUITests.swift:5-10` expects `SeedOwner`. `FolliclePause.swift:5,38` contains an unconditional 75-second sleep and is included in the test target/scheme (`iosApp.xcodeproj/project.pbxproj:35-37,131`; `xcshareddata/xcschemes/iosApp.xcscheme:45-54`).

### E. Architecture, capability, quality, and privacy debt

**TD-28 — release gates are fragmented (debt).** No tracked `.github` workflow exists. `gradle/pre-commit.sh:3-11` runs Detekt, ktlint, and iOS compilation but no shared tests. The combined command in `README.md:161-165` omits desktop tests, Android lint/build, Xcode build/UI tests, and script contracts. `build.gradle.kts:211-227` couples `quality` to externally authenticated Sonar, so there is no single offline release-confidence aggregate.

**TD-29 — migration upgrade and recovery are unproven (debt).** `shared/build.gradle.kts:247-252` derives the SQLDelight schema from migrations, but no released-database upgrade fixture/test was found for migrations 1 through 17. ADR 0018 calls for migration tests, while `shared/src/androidMain/kotlin/com/github/rodrigotimoteo/animally/domain/backup/NoOpBackupProvider.android.kt:10-13` and `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/domain/backup/NoOpBackupProvider.ios.kt:10-13` report success without a recovery backup. This is missing evidence, not proof that a current migration fails.

**TD-30 — domain/data boundaries remain coupled (debt).** `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/timeline/usecase/GetTimelineUseCase.kt:3,24,32-87` depends directly on `AnimallyDatabase`; `domain/timeline/provider/TimelineProviderRegistry.kt:3-270` imports data mappers and generated queries. `domain/backup/BackupDtos.kt`, `BackupDtosClinical.kt`, `BackupDtosRepro.kt`, `BackupRestoreBase.kt`, and `ExportBackupUseCase.kt` likewise depend on generated rows/database access. This contradicts the dependency direction described in `README.md:81-86` and raises change/test coupling; it is not itself a runtime bug.

**TD-31 — record capability metadata is duplicated (debt with demonstrated drift).** The canonical enum is `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/common/RecordType.kt:18-54`, while search indexing is manually enumerated in `data/search/SearchIndexerRegistry.kt`, export membership in `domain/export/ExportRecords.kt:22-64`, sync membership in `domain/sync/SyncEntityType.kt:16-38` plus handlers/SQL, timeline membership in `domain/timeline/provider/TimelineProviderRegistry.kt`, and native destinations/aliases in `iosApp/iosApp/Navigation/AppNavigation.swift:14-153`. TD-11 and TD-14 are concrete instances of this registry drift; the medication date gap is now covered by the repaired parity slice.

**TD-32 — host capability and route reachability diverge (debt).** Android enters `MainActivity.kt:11-18` and shared navigation at `AnimallyNavHost.kt:11-16`, whose start is `Route.PatientList` (`AnimallyNavigator.kt:14`). Owners, Settings, and Timeline are registered in `NavigationModule.kt:41-58,141-157`, but the Android root UI exposes only Search at `PatientListScreen.kt:86-106`; no production call to `Route.OwnerList`, `Route.Settings`, or global `Route.Timeline(null)` was found. iOS intentionally exposes Patients, Owners, Timeline, Search, and Assistant in `iosApp/iosApp/ContentView.swift:7-52`. This is a product-scope ambiguity, not permission to expose every registered feature.

**TD-33 — long-running assistant/bridge work lacks a complete cancellation contract (debt/audit).** `AssistantViewModel.kt:449-501` launches response work without retaining a cancellable Job; `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/presentation/ios/AssistantStore.kt:141-164`, `iosApp/iosApp/Assistant/AssistantViewModel.swift:43-61`, and `AssistantView.swift:47-75` expose no stop operation. Separately, `iosApp/iosApp/Support/StoreAwait.swift:19-38` uses an unsynchronized single-resume flag across flow and timeout tasks and does not cancel the timeout after success. The missing Stop action is established debt; the race remains an audit prompt until stress/Thread Sanitizer evidence.

**TD-34 — quality and documentation sources are stale (debt).** Detekt excludes `desktopMain` in `shared/build.gradle.kts:237-238`; the coverage floor is 57 while the declared target is 90 at `:256-268,287-290`; `config/detekt/baseline/shared.xml:5-11` retains seven baseline entries. `STRUCTURE.md:163,182,198-199` describes migrations through 14 and older entity/query counts, while the tree has 17 migrations and 31 `.sq` files; `ARCHITECTURE.md:210` also names migration 14 as latest. `scripts/update-medical-vocabulary.py:221-234` defaults to live network/year input and overwrites tracked `shared/src/commonMain/kotlin/com/github/rodrigotimoteo/animally/domain/vetreference/generated/NlmMedicalVocabulary.kt`; the generated header records URL/year but no content checksum.

**TD-35 — backup privacy intent is unresolved (decision).** `shared/src/commonMain/sqldelight/com/github/rodrigotimoteo/animally/data/migrations/10.sqm:1-5` describes assistant questions/answers and dictation transcript data as sensitive and device-local. Those records are nevertheless included in `BackupPayload.kt:96-101`, and iOS writes backup JSON to user-visible Documents in `shared/src/iosMain/kotlin/com/github/rodrigotimoteo/animally/data/backup/BackupStorage.ios.kt:16-33`. The correct fix depends on explicit product/privacy policy; the plan does not presume exclusion or encryption.

## Dependencies and ordering

The sequence is intentionally asymmetric: establish credible oracles, decide destructive/cloud semantics, secure local data, repair sync, then consolidate. Architectural extraction before behavior contracts would move defects without proving them.

1. **LUNA 0 precedes every source change.** Resolve the existing search patch and repair the minimum test/script harness needed to detect regressions.
2. **LUNA 1 is a product/security decision gate.** Backup privacy, restore-to-cloud identity, and wipe-cloud semantics determine incompatible implementations in LUNA 2 and 3.
3. **LUNA 2 local safety precedes broad sync work.** Invalid restore and false erase success are dangerous even with cloud disabled.
4. **LUNA 3 establishes sync wire and cursor correctness.** Do not enable or advertise CloudKit until all LUNA 3 acceptance tests pass.
5. **LUNA 4 joins restored/synced data to derived projections.** Search/RAG and CSV coverage depend on stable record/change semantics.
6. **LUNA 5 repairs platform reachability and lifecycle.** Expose no latent Settings/sync/reminder route before its crash, clipping, permission, and sharing defects are fixed.
7. **LUNA 6 removes duplication only after parity tests exist.** Ports/descriptors are consolidation, not substitutes for regression coverage.
8. **LUNA 7 makes the verified matrix repeatable in CI and updates documentation from observed results.**

Hard dependency graph:

```text
L0 credible baseline
  -> L1 product/privacy contracts
      -> L2 restore/backup/wipe/reminder safety
      -> L3 sync identity/delete/cursor/payload/lifecycle
          -> L4 restore-sync-search-export parity
              -> L5 platform exposure and async UX
                  -> L6 bounded consolidation
                      -> L7 release gates, migrations, documentation
```

## LUNA_EXECUTION_PLAN

LUNA means **Lowest-risk, User-data-first, Narrow, Auditable** execution. Every task is a separately reviewable vertical slice. A phase may start only when its entry criteria are met; it is complete only when its stated acceptance evidence exists. File lists below are likely surfaces, not authorization or permission to edit them.

### LUNA 0 — establish a trustworthy baseline

#### LUNA 0.1 — resolve the in-flight medication index patch (completed)

- **Depends on:** the existing dirty changes were preserved as user-owned work.
- **Work:** review only the current search/medication diff, run its focused tests, and either integrate it intact or discard it by an explicit owner decision. Preserve index-version compatibility.
- **Likely surfaces:** the seven user-owned files listed in the baseline section.
- **Acceptance:** medication save-time and bulk reindex both use `startDate`; the version-23 migration heals existing rows; no unrelated dirty change is absorbed. **Result:** met.
- **Verification:** `./gradlew :shared:testAndroidHostTest --tests '*RecordTypeIndexingTest*' --tests '*SaveMedicationUseCaseTest*'`, then iOS compilation, ktlint, and Detekt.

#### LUNA 0.2 — replace the SQLDelight count oracle with an explicit contract (completed)

- **Depends on:** none.
- **Work:** represent the intentionally direct-owned query sets as an explicit allowlist/map; validate missing, duplicate, and wrong bindings; wire the check into a maintained local/CI gate.
- **Likely surfaces:** `scripts/check-queries-module.sh`, `QueriesModule.kt`, build/CI entry point, focused script fixture if needed.
- **Acceptance:** the current intentional Insights ownership passes; deleting or duplicating one normal binding fails with a precise name. **Result:** met by the real-tree check and fixture suite.
- **Verification:** `bash -n scripts/check-queries-module.sh`; `scripts/check-queries-module.sh` exits zero on the real tree and nonzero in focused negative fixtures.

#### LUNA 0.3 — make simulator build/boot/install deterministic (script portion completed)

- **Depends on:** a healthy CoreSimulatorService and an available runtime.
- **Work:** discover/select a device, use a task-owned DerivedData path, propagate the actual `.app`, fail on boot/install errors, and align documented command/env names.
- **Likely surfaces:** `scripts/sim-e2e.sh`, `scripts/README-e2e.md`.
- **Acceptance:** the script now discovers a supported device, owns DerivedData/output paths, and propagates boot/install errors without editing. **Result:** script syntax/help passed; live boot/install/screenshot remain blocked by CoreSimulatorService.
- **Verification:** `bash -n scripts/sim-e2e.sh`; run documented `boot`, `build`, `install`, `launch`, and `screenshot` sequence.

#### LUNA 0.4 — make iOS UI tests isolated (partial)

- **Depends on:** LUNA 0.3.
- **Work:** remove the unconditional 75-second exploratory `FolliclePause` test and retain isolation/reset work for a healthy simulator.
- **Acceptance:** no unconditional sleep remains in the maintained UI suite. The twice-on-a-fresh-simulator evidence is pending the CoreSimulatorService repair.
- **Verification:** a narrow `xcodebuild test -only-testing:...` run twice after erasing the simulator, followed by the full maintained UI scheme.

### LUNA 1 — freeze product, privacy, and compatibility contracts

#### LUNA 1.1 — publish the supported capability matrix

- **Work:** decide which hosts support Owners, Timeline, Settings, Assistant, Insights, reminders, CSV/PDF/backup, CloudKit, Embryo Transfer, and ICSI. Label unavailable capabilities as intentionally unsupported rather than leaving unreachable registrations or misleading controls.
- **Acceptance:** README/product navigation/settings agree with executable routes for Android and iOS.

#### LUNA 1.2 — decide dataset identity semantics

- **Work:** specify whether JSON/package restore (a) rejoins existing cloud identities, (b) imports as a new dataset, or (c) requires cloud to be disabled/reset; specify whether wipe means local-only, all devices/cloud, or requires a choice.
- **Acceptance:** explicit truth table for restore/wipe versus `serverId`, export cursor, imported state, pending tombstones, and cloud feature flag. Product copy matches it.
- **Blocks:** LUNA 2.3, 2.7, 3.2-3.4, 4.1.

#### LUNA 1.3 — decide backup privacy and portability

- **Work:** decide inclusion/encryption/retention for patient photos, clinical images, dictation audio, assistant Q/A, and transcripts; classify user-visible Documents versus protected app storage; define maximum package size and missing-media policy.
- **Acceptance:** a reviewed backup manifest/privacy policy and user-facing disclosure. No implementation begins while “complete backup” and privacy requirements conflict.

#### LUNA 1.4 — freeze sync wire compatibility

- **Work:** document required/optional parent semantics, tombstone format, nullable clearing, payload versioning, entity dependency order, and retry/cursor rules for every `SyncEntityType`.
- **Acceptance:** contract fixtures cover all currently supported entity types and reject incompatible versions without mutating local data.

### LUNA 2 — secure local backup, restore, erase, and reminders

#### LUNA 2.1 — add non-destructive restore preflight

- **Depends on:** LUNA 1.2-1.3.
- **Work:** parse into a staged model and validate schema capability, IDs/uniqueness, owner-patient-child references, ultrasound-follicle references, active-row rules, dates/counts, manifest entries, checksums, and capacity before opening the destructive transaction.
- **Acceptance:** invalid/corrupt/oversized/forward-version backups produce typed errors and leave database, files, sync state, and FTS byte-for-byte/logically unchanged.

#### LUNA 2.2 — introduce monotonic backup compatibility

- **Depends on:** LUNA 2.1.
- **Work:** increment the format for every material field/entity change or add an explicit capability/min-reader manifest; implement versioned readers/migrators; reject forward-incompatible packages. Retain read support for existing version-1 JSON.
- **Acceptance:** old-v1 import passes; a newer unsupported package is rejected before clearing; each supported migration has a fixture.

#### LUNA 2.3 — make restore atomic and outcomes truthful

- **Depends on:** LUNA 1.2, 2.1-2.2.
- **Work:** show a preflight summary and explicit destructive confirmation; stage media; take a recoverable local snapshot; replace rows transactionally; align/reset sync and search state per policy; rebuild/invalidate projections; publish distinct `Committed`, `CommittedWithRecoveryNeeded`, and `Rejected/RolledBack` outcomes.
- **Acceptance:** failures before commit preserve old state; failures after commit never claim that no change occurred; retry is idempotent; UI presents counts/format/source and confirmation.

#### LUNA 2.4 — capture one point-in-time snapshot

- **Depends on:** LUNA 2.2.
- **Work:** use a database read transaction/checkpoint/supported SQLite backup mechanism so all DTO collections describe one snapshot; make any raw database artifact use a proven WAL/journal-safe mechanism; bind artifacts to one manifest/snapshot ID.
- **Acceptance:** concurrent save during export yields either wholly-before or wholly-after state, never mixed parents/children; JSON and raw artifacts identify the same snapshot.

#### LUNA 2.5 — create a portable backup package

- **Depends on:** LUNA 1.3, 2.2, 2.4.
- **Work:** package versioned data plus media manifest, attachment/audio bytes, content hashes, logical IDs, sizes, and privacy metadata. Never treat container-absolute paths as portable identity.
- **Acceptance:** package is self-contained, validates before restore, detects missing/tampered entries, and maintains configured size limits without partial success.

#### LUNA 2.6 — import media and rewrite paths safely

- **Depends on:** LUNA 2.3, 2.5.
- **Work:** stage imported media under generated safe names, verify types/sizes/hashes, rewrite restored rows only to committed local paths, deduplicate deliberately, and clean staging/partial files on error.
- **Acceptance:** restore into a new container renders every included photo/image/audio; corrupt/missing media follows the declared policy; rollback leaves no staged files.

#### LUNA 2.7 — make erase semantics complete and auditable

- **Depends on:** LUNA 1.2-1.3; coordinate with LUNA 3 tombstones if cloud deletion is required.
- **Work:** enumerate dictation, patient photos, imaging/ultrasound attachments, export/staging artifacts, and applicable secure/preferences/sync state; cancel scheduled notifications; require successful deletion or return a typed residual-items result with retry. Apply cloud reset/delete policy.
- **Acceptance:** post-wipe database, FTS, files, notifications, and defined cloud state are empty; injected `false`/exception paths never produce an unconditional success message; subsequent sync does not resurrect records.

#### LUNA 2.8 — close reminder lifecycle

- **Depends on:** LUNA 1.1.
- **Work:** persist enablement; add cancel/reschedule; reconcile create/edit/delete/disable/wipe/permission/time changes; keep scheduling at the platform edge.
- **Acceptance:** deleted or disabled reminders cannot fire, restart preserves preference, permission denial is explicit, and a reconciliation test covers stale OS requests.

### LUNA 3 — repair sync correctness before exposure

#### LUNA 3.1 — build deterministic engine contract tests

- **Depends on:** LUNA 0 and 1.4.
- **Work:** fake the native/remote event bridge and clock; test partial success, missing callback, cancellation, duplicate callback, retry, parent resolution, tombstones, and import/export ordering without a live account.
- **Acceptance:** every known TD-08 through TD-12 and TD-22 trigger starts as a failing focused test.

#### LUNA 3.2 — distinguish absent optional parents from unresolved required parents

- **Depends on:** LUNA 1.4, 3.1.
- **Work:** encode optional-null, explicit unlink, and unresolved-server-ID as distinct states; allow ownerless patients; preserve dependency deferral only for genuinely unresolved parents.
- **Acceptance:** ownerless patient exports/imports, owner unlink round-trips, and all children sync after the patient receives identity.

#### LUNA 3.3 — make tombstones first-class

- **Depends on:** LUNA 1.2, 1.4, 3.1.
- **Work:** query changed rows regardless of active status, build deleted records from raw rows, order child/parent deletion safely, and define retention/compaction. Do not route tombstones through active-only repository APIs.
- **Acceptance:** delete for every supported entity reaches the remote and a second device; retries are idempotent; wipe policy test prevents resurrection.

#### LUNA 3.4 — make cursors retry-safe

- **Depends on:** LUNA 3.1.
- **Work:** advance only through the highest contiguous acknowledged position, or persist explicit failed work; use a stable tie-breaker for equal timestamps; never skip an unacknowledged row.
- **Acceptance:** `t1` failure plus `t2` success retries `t1`; equal-timestamp batches neither duplicate nor omit; cancellation leaves a safe cursor.

#### LUNA 3.5 — complete and version reproductive payloads

- **Depends on:** LUNA 1.4, 3.2-3.4.
- **Work:** add the missing reproduction and ultrasound fields; introduce Follicle with ultrasound dependency, tombstone behavior, server-ID assignment, registry/tracker/order entries, and backward-compatible payload versioning.
- **Acceptance:** field-by-field round trips preserve all structured values and multi-follicle records; older payload behavior is explicit and tested.

#### LUNA 3.6 — bound CloudKit lifecycle and failure

- **Depends on:** LUNA 3.1-3.5.
- **Work:** make native-engine start/stop ownership explicit, use structured scope/cancellation, bound waits and queue growth, make callbacks single-completion, surface account/entitlement/offline state, and make start idempotent.
- **Acceptance:** missing callback times out to a retryable state, cancellation releases the mutex, repeated start is safe, and no flow reports perpetual syncing.

#### LUNA 3.7 — choose ship or hide

- **Depends on:** all prior LUNA 3 tasks plus live integration evidence.
- **Ship branch:** add intentional opt-in/status/account lifecycle UI only on supported hosts and run live CloudKit tests.
- **Hide branch:** remove misleading controls/claims, keep implementation explicitly experimental/disabled, and prevent persisted flags from entering it.
- **Acceptance:** no host advertises a sync capability that cannot complete its supported contract. The disabled generic Ktor backend remains out of scope.

### LUNA 4 — align restore, search/RAG, timeline, and export projections

#### LUNA 4.1 — join restore and sync identity safely

- **Depends on:** LUNA 1.2, 2.3, 3.2-3.4.
- **Work:** implement the selected new-dataset/rejoin/reset policy atomically across row `serverId`s, `SyncMetadata`, `SyncState`, CloudKit cursor/import state, tombstones, and feature flags.
- **Acceptance:** restore followed by sync creates no duplicates, skips no unchanged rows incorrectly, and is idempotent across interruption/retry.

#### LUNA 4.2 — make remote application maintain derived state

- **Depends on:** LUNA 3.2-3.5.
- **Work:** route imported mutations through a transactionally coherent mutation/index side effect or mark the index dirty once per batch and rebuild after commit. Preserve cancellation and avoid per-row full rebuilds.
- **Acceptance:** remote insert/update/delete is immediately reflected in global search and patient-scoped RAG evidence; a failed batch cannot leave a healthy marker over stale FTS.

#### LUNA 4.3 — strengthen index health and Android startup ownership

- **Depends on:** LUNA 0.1, 4.2.
- **Work:** validate both metadata and FTS projections, counts/integrity rather than version alone; make reindex exclusion/thread ownership explicit; invoke the same boot contract on Android and iOS off the UI-critical path with observable progress/recovery.
- **Acceptance:** current-version metadata plus empty FTS self-heals; concurrent write/reindex cannot lose rows; both hosts upgrade from prior index versions.

#### LUNA 4.4 — make export coverage explicit

- **Depends on:** stable record set from LUNA 3.5.
- **Work:** include Custom Reminder, Embryo Transfer, ICSI, Follicles, and any other declared exportable record; define active/date/denominator semantics; add a canonical coverage test comparing supported record capabilities to emitted CSV sections.
- **Acceptance:** one fixture containing every exportable record produces one traceable section/row per record; intentional exclusions are named in product copy and tests.

#### LUNA 4.5 — add record-capability parity checks

- **Depends on:** LUNA 4.2-4.4.
- **Work:** create a compile-time/test-visible descriptor or parity matrix for each `RecordType`: owner/patient relationship, canonical event date, search text/indexing, timeline provider, CSV/backup inclusion, sync handler/order, Swift destination/icon. Generate only where it reduces duplication without hiding domain rules.
- **Acceptance:** adding a record type or field fails a focused test until every required projection is explicitly implemented or exempted with rationale.

### LUNA 5 — repair platform reachability, lifecycle, and async UX

#### LUNA 5.1 — define and verify Android navigation before exposing latent screens

- **Depends on:** LUNA 1.1, 4.3.
- **Work:** establish intentional top-level destinations; keep unsupported features hidden; make Settings scrollable and test compact-height reachability.
- **Acceptance:** every advertised Android capability has a reachable route/back-stack contract and its lower Settings actions are operable.

#### LUNA 5.2 — use typed search destinations

- **Work:** retain entity and record identity through mapping, grouping, and click handling; navigate OWNER to owner detail, PATIENT to patient detail, and records to the appropriate patient/record destination.
- **Acceptance:** owner/patient ID collision does not merge groups or misnavigate; parity tests cover every searchable entity.

#### LUNA 5.3 — separate date text from parsed filters

- **Work:** keep raw field text during editing, validate/parse on commit or when complete, show invalid-range feedback, and preserve the last intentional filter.
- **Acceptance:** incremental typing, deletion, paste, invalid dates, and `from > to` are deterministic in a Compose UI test.

#### LUNA 5.4 — move sharing to an Activity-capable edge

- **Work:** launch share from an Activity/result-capable UI boundary or apply the explicit new-task contract where product-approved; validate FileProvider grants and missing reader behavior.
- **Acceptance:** CSV, PDF, and backup share open from a release Android build without context exceptions.

#### LUNA 5.5 — request notification permission at the UI boundary

- **Depends on:** LUNA 2.8.
- **Work:** use an Activity result launcher, feed the result back to the shared state machine, and handle denial/permanent denial/settings return.
- **Acceptance:** clean Android 13+ install shows the system prompt once and state matches the result.

#### LUNA 5.6 — move long settings operations off the main actor

- **Depends on:** LUNA 2.3-2.6.
- **Work:** give export/restore/PDF/wipe typed progress and structured cancellation; own dispatchers in the implementation layer; keep UI mutations on main; prevent duplicate submissions.
- **Acceptance:** maximum allowed backup does not block UI responsiveness, cancellation is coherent, and completion accurately distinguishes commit/recovery/rejection.

#### LUNA 5.7 — add cancellable assistant work and harden bridge awaiting

- **Work:** retain/cancel the assistant Job through shared and Swift stores; expose Stop; preserve partial/transient state policy; make `StoreAwait` single-resume, timeout-cancelling, and parent-cancellation safe.
- **Acceptance:** stop terminates provider/retrieval work without a late state overwrite; timeout and completion races resume exactly once under stress/Thread Sanitizer testing.

### LUNA 6 — bounded architectural consolidation

#### LUNA 6.1 — consolidate mutation side effects

- **Depends on:** LUNA 4 parity tests.
- **Work:** define one transaction boundary for a domain mutation plus required search, sync-change, and notification effects, while keeping platform I/O at edges. Avoid a universal repository or event bus unless measured needs justify it.
- **Acceptance:** save/delete/import paths cannot bypass declared projections; cancellation and soft-delete semantics remain covered.

#### LUNA 6.2 — restore domain/data boundaries

- **Depends on:** LUNA 2 and 4 stable behavior.
- **Work:** introduce narrow snapshot/restore and timeline repository ports in common domain; move SQLDelight row reading, generated query types, and data mappers behind data implementations. Do not split modules as part of this slice.
- **Evidence motivating work:** `domain/timeline/usecase/GetTimelineUseCase.kt:3,24,32-87`, `domain/timeline/provider/TimelineProviderRegistry.kt:3-270`, `domain/backup/BackupDtos*.kt`, `BackupRestoreBase.kt`, and `ExportBackupUseCase.kt` import data/generated database types.
- **Acceptance:** common domain contracts compile/test without generated SQLDelight row types; behavior and query count remain unchanged.

#### LUNA 6.3 — eliminate proven registry drift

- **Depends on:** LUNA 4.5.
- **Work:** derive or co-locate only stable metadata now duplicated between `RecordType`, search indexers, timeline providers, export/sync registries, and Swift metadata. Retain specialized domain transformations.
- **Acceptance:** the parity test, not developer memory, identifies an incomplete new record type; no reflection/runtime string registry is introduced.

### LUNA 7 — institutionalize verification and maintenance

#### LUNA 7.1 — add a tracked CI matrix and offline aggregate

- **Depends on:** LUNA 0 repairs.
- **Work:** create fast PR lanes for formatting/static checks/focused shared tests and host lanes for Android build/lint, desktop tests, iOS compile/test, script contracts, and artifact checks. Keep Sonar optional/separate because it requires external credentials.
- **Acceptance:** a clean checkout runs the documented matrix; no release-critical suite exists only in local convention.

#### LUNA 7.2 — add migration fixtures and recovery evidence

- **Work:** preserve representative databases from released schema versions, upgrade each to current, validate counts/relationships/search/sync metadata, and test failure recovery. First discover the actual SQLDelight migration task rather than assuming its name.
- **Acceptance:** all 17 migrations are covered by at least the supported release-window fixtures; backup-before-migration/recovery behavior matches ADR policy on Android and iOS.

#### LUNA 7.3 — repair static-analysis and coverage truth

- **Work:** include `desktopMain` in Detekt or document exclusion, remove stale baseline entries, replace unexplained suppressions with local rationale, and ratchet Kover from the measured current value rather than jumping directly from 57% to 90%.
- **Acceptance:** baseline contains only current findings; each release cannot lower the ratcheted floor; generated code is explicitly excluded or verified.

#### LUNA 7.4 — make generated vocabulary reproducible

- **Work:** pin source year/URL/content checksum, support offline regeneration from a cached source artifact, verify deterministic output, and measure compile impact before relocating/splitting the 29k-line generated Kotlin file.
- **Acceptance:** identical input produces byte-identical output; provenance and update review are visible; no live-network result is silently committed.

#### LUNA 7.5 — reconcile documentation with shipped reality

- **Depends on:** observed results of prior phases.
- **Work:** update ADR statuses, schema/migration/entity counts, host capability matrix, backup/privacy guarantees, sync status, verification commands, and intentionally unsupported desktop paths.
- **Acceptance:** README, `ARCHITECTURE.md`, `STRUCTURE.md`, ADRs, scripts, and UI labels agree with the verified implementation.

## Original verification routes (retained for deferred work)

These commands were the plan's proposed routes. The executed evidence is recorded near the top of this document; retain the routes below for the deferred policy, migration, live-service, and simulator work. Commands that mutate external services still require the relevant product/service authorization.

### Planning/documentation checks

```sh
git diff --check -- docs/maintenance/TECH_DEBT_PLAN.md
git status --short
```

### Resolved medication/search baseline

```sh
./gradlew :shared:testAndroidHostTest \
  --tests '*RecordTypeIndexingTest*' \
  --tests '*SaveMedicationUseCaseTest*'
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:ktlintCheck :shared:detekt
```

### Script and simulator harness

```sh
bash -n scripts/check-queries-module.sh scripts/sim-e2e.sh
sh -n gradle/pre-commit.sh
scripts/check-queries-module.sh
xcrun simctl list devices available
```

### Shared correctness gate

```sh
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:desktopTest
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:ktlintCheck :shared:detekt :shared:koverVerify
```

Add focused test filters before the broad gate for:

- invalid/orphan/duplicate/forward-version restore rejection with no mutation;
- v1-to-current backup fixture migration and cross-container media restore;
- export during a concurrent write and WAL-safe raw snapshot validation;
- restore followed by sync without duplicates or skipped exports;
- wipe followed by filesystem inspection, notification inspection, and sync without resurrection;
- ownerless patient and owner-unlink sync;
- tombstone round trip for every sync entity;
- `t1` failure/`t2` success and equal-timestamp cursor cases;
- complete reproduction/ultrasound/follicle payload round trip;
- remote apply followed by search/RAG;
- current-version metadata with empty FTS startup healing on both hosts;
- CSV fixture containing every supported record type.

### Android host

```sh
./gradlew :androidApp:lintDebug :androidApp:assembleDebug
```

Instrument/manual evidence required: clean Android 13+ notification prompt; owner/patient ID collision; incremental date typing; compact-height Settings scrolling; CSV/PDF/backup sharing; startup healing after index corruption; reminder delete/disable/wipe reconciliation.

### iOS host and UI

```sh
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  build

xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,id=<discovered-available-udid>' \
  -only-testing:AnimallyUITests/AnimallyUITests/testLaunchShowsPatientList \
  test
```

For UI isolation, erase/reset through the authorized harness and run the narrow test twice. Live CloudKit verification additionally requires a valid entitlement/container/account and must cover offline, account change, partial failure, callback loss, cancellation, and multi-device delete/import behavior.

### Migration-task discovery

```sh
./gradlew :shared:tasks --all
```

Filter the output for SQLDelight/migration tasks and record the exact supported command in the maintained README/CI. Do not document `verifySqlDelightMigration` unless task discovery proves it exists.

## Resolved and remaining blockers

1. **Resolved — user-owned dirty search patch:** preserved and validated with the focused/full shared matrix.
2. **CoreSimulatorService:** the earlier `xcrun simctl list devices available` attempt reported an invalid/refused CoreSimulatorService connection. No fresh simulator/UI result is valid until the runtime/device is healthy.
3. **Cloud dataset policy:** restore identity and wipe semantics require a product decision (rejoin, new dataset, local-only, or delete cloud) before implementation.
4. **Backup privacy policy:** inclusion and protection of media, dictation, assistant questions/answers, and transcripts require an explicit decision.
5. **SQLite snapshot mode:** current Android/iOS journal/WAL behavior must be measured before selecting a raw-database copy/checkpoint mechanism.
6. **CloudKit integration:** container entitlements, CKSyncEngine callback behavior, account transitions, and persisted Swift import state require live iOS evidence after deterministic engine tests.
7. **CI/external services:** Sonar requires external credentials/network and must not block the offline correctness aggregate.
8. **Migration fixtures:** released-schema databases and the supported compatibility window must be identified before claiming migration safety.

## Deferred scope after the implementation baseline

- Remaining LUNA items are intentionally deferred until the product, privacy, storage, simulator, or CI blockers above are resolved; this record does not imply completion of those items.
- The generic Ktor sync backend, which is intentionally disabled; maintenance work must not invent a server.
- A wholesale module split, universal repository/event bus, or broad rewrite. Narrow ports and parity tests come first.
- Immediate 90% coverage. Use risk-based missing tests and a measured non-decreasing ratchet.
- New production dependencies, dependency upgrades, lockfiles, or Gradle wrapper checksum policy until separately scoped. Current duplication of Detekt 1.23.8, hard-coded Compose UI test 1.11.1, and absent dependency verification are hygiene items, not this critical path.
- Database-level foreign keys/cascades. ADR 0010 intentionally chose app-layer enforcement; restore preflight and mutation contracts should be fixed before reconsidering the storage policy.
- Productizing desktop share/PDF/notifications/secure storage; current desktop paths are POC/no-op and desktop is not established as a supported product host.
- Large-file splitting solely by line count (`SpeechTranscriberService.swift`, `AssistantView.swift`, `GenerateRagResponseUseCase.kt`, `SearchIndexerRegistry.kt`, and related hotspots). Split only after change-frequency, compile-time, or defect evidence identifies a bounded seam.
- Moving/splitting `NlmMedicalVocabulary.kt` before deterministic generation and compile-impact measurement.
- A general logging framework. First define privacy-redacted operational events for backup/restore/wipe/sync/index recovery; broader observability requires separate scope.
- Common-word patient-name collisions in RAG (`RagRetriever.kt:240-248`), `SaveFolliclesUseCase` non-transactional replacement (`:27-39`), duplicate server IDs for migration-7 entities, same-day backup overwrite, `StoreAwait` races, iOS main-thread reindex timing, and empty-byte iOS `addressOf(0)`: retain as audit prompts until focused reproduction/profiling promotes them.
- Force-unwrap cleanup or suppression removal without a concrete failure. Suppressions are audit prompts, not automatic bugs.
- Reopening completed historical cleanup from `.slim/deepwork/integrity-simplify.md` unless a current defect proves regression.

## Original completion definition

The broader plan is not yet implementation-complete. Its remaining acceptance conditions are:

- every material pain point has a classification, evidence path, dependency, and disposition;
- every P0/P1 execution item has an acceptance check and narrow verification route;
- product/privacy decisions are resolved before incompatible implementation begins;
- the offline host/test matrix and deterministic UI harness pass from a clean checkout;
- live CloudKit capability is either verified and intentionally exposed or explicitly hidden;
- documentation is updated from observed behavior, not aspiration;
- no implementation is inferred from approval of this document alone.

The current implementation baseline is complete for the deterministic/local slices listed in the execution record. The remaining unchecked conditions are live simulator/UI evidence, Android runtime permission evidence, CloudKit integration/policy, portable backup media, migration fixtures, CI, and the deferred architectural/documentation consolidation work.
