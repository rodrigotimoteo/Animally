# Implementation Plan: iOS-first product polish

## Overview

Add a persisted accent-colour choice, improve the Patients and Owners workflows,
make dictation genuinely testable without microphone or Apple Intelligence
dependencies, and harden the assistant's cloud/local behavior and conversational
presentation. Shared Kotlin remains the home for persistence, domain rules,
view-model state, and assistant/dictation decisions; Swift remains the iOS
presentation and platform-integration layer.

## Architecture decisions

- Store theme mode and accent choice through the shared `ThemePreferenceStore`
  contract, with platform implementations persisting the values locally.
- Keep accent-to-colour mapping in the presentation layer; no database or
  record-domain logic belongs in SwiftUI.
- Use injectable dictation protocols and a deterministic fixture path for
  simulator/CI coverage; reserve microphone/SpeechAnalyzer validation for a
  connected real device.
- Improve assistant behavior at the shared RAG/stream boundary: preserve
  grounding and dosage safety, normalize provider output, and use concise,
  warm language in the UI.

## Task list

### Phase 1: Foundation

- [x] Add an `AccentColor` preference and persist it on Android, desktop, and iOS.
- [x] Expose the preference through the shared settings view model/store and
  apply it to the iOS tint and Compose theme.

### Phase 2: iOS UX

- [x] Add a clear accent picker and preview to Settings.
- [x] Improve Patients and Owners list/detail hierarchy, empty/loading/error
  states, search/discoverability, and destructive-action confirmation without
  moving data logic into SwiftUI.

### Phase 3: Dictation and assistant

- [x] Add a deterministic simulator dictation route that exercises transcript,
  extraction, review, validation, disambiguation, and save states.
- [x] Add focused assistant regression coverage for cloud responses, thinking
  block removal, truncation/error states, and human-readable fallback copy.
- [x] Polish assistant feedback and response presentation while retaining
  source transparency and retry behavior.

### Checkpoints

- [x] Shared tests and static analysis pass after each cross-platform foundation.
- [x] iOS simulator build and UI smoke tests pass after the UX/dictation slices.
- [ ] Final device build installs and launches on Daniela's iPhone.
- [x] Final device build succeeds; the final reinstall/launch is blocked while
  Daniela's iPhone CoreDevice tunnel is unavailable.
- [x] Working tree is clean after the requested commit.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Accent preference only applies to part of the app | Confusing visual inconsistency | Route tint and shared theme colors through one preference and test persistence/defaults |
| Simulator has no reliable live speech input | Dictation coverage is false confidence | Test the deterministic extractor/review path separately and report live speech as device-only |
| Assistant polish weakens safety or grounding | Incorrect clinical guidance | Keep dosage refusal, record citations, and cloud/on-device policy tests unchanged |
| SwiftUI changes duplicate Kotlin state | Stale or divergent behavior | Swift calls stores and renders published state; shared Kotlin owns decisions |

## Open questions

- The exact cloud provider/model used in production can still change, so live
  provider testing should remain an optional smoke test with a temporary key.

---

# Current implementation slice: dictation theme lifecycle and assistant history

## Overview

Make the dictation sheet retain the selected accent whenever it is opened,
closed, and opened again, and make the existing local assistant history
discoverable and useful on iOS. The history remains a Kotlin-owned list of up
to 15 completed question/answer turns; SwiftUI only renders the iOS projection,
formats dates, and requests that a saved question be reused in the composer.

## Confirmed findings

- `AssistantViewModel` already persists and hydrates the newest 15 completed
  turns in shared Kotlin, but `AssistantView` exposes only the current transcript
  and the separate dictation archive button.
- The persisted model is turn-based, not session-based. The UI must therefore
  call this “Chat history” or “Recent chats” and must not imply that separate
  named conversations can be reopened.
- `DictationCaptureView` uses `Theme.forestGreen` (`Color.accentColor`) but the
  sheet does not explicitly receive `ThemeViewModel.accentColor`. That relies
  on inherited SwiftUI tint propagation and is vulnerable to stale sheet
  presentation state after a recording attempt and re-entry.

## Architecture decisions

- Add the recent-turn collection to shared assistant state and refresh it after
  a completed answer is persisted. The Kotlin ViewModel remains responsible for
  ordering, retention, loading, and error state.
- Project history to an Objective-C-friendly iOS store state with primitive
  fields, including epoch milliseconds, following the existing dictation-store
  bridge pattern. No Swift database or repository access is introduced.
- Add a clearly separate “Chat history” toolbar action. A list shows the newest
  turns first, searchable by question/answer; a detail screen shows the complete
  saved exchange and offers “Use question” to place it back in the composer for
  editing before sending.
- Apply the current theme tint explicitly to the assistant’s dictation and
  history sheets. The persisted accent source remains the shared theme store;
  Swift only supplies the current presentation color.

## Dependency graph

```text
AssistantChatHistoryRepository / use cases
        |
        v
AssistantViewModel state + refresh after save
        |
        v
AssistantStore iOS projection (turns + epoch dates)
        |
        +--> Chat history list/detail/reuse UI
        |
        +--> explicit live tint on assistant sheets
```

## Acceptance criteria

1. Selecting an accent, opening dictation, starting/cancelling or failing a
   recording, closing the sheet, and opening it again keeps the selected accent
   on the sheet controls.
2. The assistant toolbar exposes a distinct Chat history action without
   confusing it with Dictation history.
3. Chat history shows up to the 15 persisted turns newest first, supports local
   search, exposes the full question and answer on selection, and clearly marks
   cloud/on-device and partial responses.
4. “Use question” closes history and fills the assistant composer without
   sending unexpectedly; the user can edit and submit it normally.
5. A newly completed answer appears in Chat history without requiring an app
   restart; persistence failures remain visible through the existing shared
   history-error state.
6. Kotlin retention/order behavior remains unchanged, dictation archive behavior
   remains separate, and no database or domain logic is added to Swift.

## Verification plan

- Focused shared retention test plus a state-level test or compile check for
  history refresh/projection.
- `git diff --check`, shared `iosSimulatorArm64Test`, ktlint, and detekt.
- iOS simulator build, then focused UI coverage for opening Chat history,
  viewing/reusing a turn when seeded data exists, and reopening dictation after
  the deterministic start/cancel path.
- Manual simulator screenshots after changing the accent to verify the first
  and second dictation presentations have the same current tint. Live microphone
  recognition remains a physical-device-only limitation.

## Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Existing data has turns but no conversation/session id | Users could be misled about what can be reopened | Present individual saved exchanges as recent turns, not named sessions |
| Swift sheet captures an old environment value | Accent appears to revert after re-entry | Bind `.tint` directly to the live `ThemeViewModel` on every sheet presentation |
| Kotlin collections/types are awkward in Objective-C headers | iOS build failure or fragile Swift access | Use a small `@ObjCName` primitive projection, matching `DictationStore` |
| A history refresh races with generation | Composer could be disabled or transcript replaced | Preserve live messages on refresh and block only while the shared load is active |

## Current implementation slice: synthetic equine fixture and cloud assistant validation

### Confirmed requirements

- The real `backup.json` contains personal data and must remain untouched.
- The existing iOS restore flow accepts a complete schema-v1 backup as pasted JSON,
  so a separate fixture can populate a development simulator without adding a
  production-only seed mode or moving persistence logic into Swift.
- The cloud assistant has four deterministic analysis tools (patient census,
  weight summary, preventive care, and gestation summary), plus ordinary grounded
  retrieval and source-card emission. The fixture must exercise all of them.
- Pregnancy day counts are projected from the breeding date in shared Kotlin, so
  the fixture's dates should be coherent as of 2026-08-27 while the app remains
  responsible for live recalculation.

### Architecture decisions

- Add `fixtures/demo-equine-herd.json` as an explicitly synthetic, importable
  backup payload. Keep assistant and dictation history empty so cloud tests start
  from a known state.
- Use fictional owners, horses, identifiers, and clinicians; do not copy names,
  contact details, or file paths from the personal backup.
- Cover two active positive gestations, a resolved/negative reproductive path,
  weight trends, vaccinations, deworming, farrier care, consultations, a lab
  result, medication, lameness, and reproductive ultrasound/event records. Keep
  the pregnant horses free of unsupported emergency diagnoses so a groundedness
  probe can verify the assistant does not transfer facts between patients.
- Validate the fixture structurally with JSON tooling, restore it through the
  actual iOS Settings flow, and drive cloud questions through the actual
  assistant UI. Use the supplied OpenRouter key only transiently; never store it
  in source, logs, task notes, or simulator artifacts.

### Dependency graph

```text
Synthetic backup fixture
        |
        v
iOS Settings -> Restore Backup -> shared restore + search index
        |
        v
Assistant retrieval / Kotlin analysis tools
        |
        v
OpenRouter cloud stream -> answer + source cards in Swift UI
```

### Acceptance criteria

1. The demo fixture is valid schema-v1 JSON, contains four coherent horses and
   owners, and the restore attempt either succeeds or leaves an explicit
   simulator-automation limitation recorded.
2. The restored simulator exposes two active pregnancies with the expected
   breeding dates/due dates and enough records to answer census, weight, care,
   gestation, and patient-specific questions.
3. Cloud smoke/evaluation questions confirm correct counts and dates when the
   provider accepts requests; source cards map only to returned records,
   tool-backed analysis is covered by shared tests, and unsupported patient
   facts are refused rather than invented.
4. English and Portuguese questions work with the configured cloud model, inline
   reasoning is not displayed, and normal/partial/error responses remain usable.
5. The API key is not persisted in the repository or test output, and all code,
   fixture, and test changes are committed with a clean worktree.

### Verification plan

- Parse the fixture with `jq`, check foreign-key references and expected entity
  counts with a bounded script, then restore it in the iOS simulator through the
  real Settings screen.
- Run the shared iOS simulator tests, Kotlin static analysis, and the iOS native
  build after any code changes.
- Configure OpenRouter in the app with the temporary key and a suitable available
  model; run a small matrix of factual, analysis/tool, citation/card, negative,
  bilingual, and long/streaming questions. Capture answer text and visible source
  cards without recording the secret.
- If a failure appears, first reproduce it against the same fixture and request,
  then make the smallest Kotlin-side fix and rerun the focused checks.

### Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Synthetic records look like clinical advice | Test results could be mistaken for real patient history | Label the fixture and test install as demo data; use fictional identities and conservative notes |
| Fixture violates a schema or relationship | Restore fails or analysis tools see incomplete rows | Build from the backup DTO contract, validate JSON/foreign keys, and restore through the production flow |
| Cloud model invents a patient fact | Unsafe assistant behavior | Probe absent facts per horse and require the app's deterministic no-results response/source behavior |
| Model/provider consumes the key or quota unexpectedly | User loses access or secret leaks | Use only a bounded request matrix, no background loops, never write the key to disk or logs, and clear app configuration afterward if possible |

### Evaluation outcome

- The synthetic fixture parses cleanly and was visible in the iOS patient list; the
  full restore button interaction was left unconfirmed after the simulator text
  editor captured coordinate taps.
- Current gestation questions now bypass model arithmetic and returned live day
  counts, due dates, and matching source cards in the simulator. This prevents a
  cloud or local model from repeating stale stored progress.
- The cloud route was selected and displayed its cloud badge, but the supplied
  OpenRouter key returned HTTP 403 (`Key limit exceeded`) during the analysis
  probe. Further live requests were intentionally stopped to avoid quota abuse.
- Shared unit tests cover tool-backed/grounded refusals and Portuguese current
  gestation output; live provider availability remains an external dependency.
