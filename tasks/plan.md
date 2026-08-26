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
