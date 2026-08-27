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
