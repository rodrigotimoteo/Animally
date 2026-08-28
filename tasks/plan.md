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
discoverable and useful on iOS. The history remains Kotlin-owned: up to 15
completed question/answer turns are persisted, each new chat gets a stable
conversation boundary, and SwiftUI only renders the grouped iOS projection,
formats dates, and requests that a saved question be reused in the composer.

## Confirmed findings

- `AssistantViewModel` already persists and hydrates the newest 15 completed
  turns in shared Kotlin, but `AssistantView` exposes only the current transcript
  and the separate dictation archive button.
- The persisted model was initially turn-based, not session-based. A migration
  now adds a stable conversation id while assigning legacy rows to individual
  safe groups, so the UI can reopen complete multi-turn blocks without merging
  unrelated old answers.
- `DictationCaptureView` uses `Theme.forestGreen` (`Color.accentColor`) but the
  sheet does not explicitly receive `ThemeViewModel.accentColor`. That relies
  on inherited SwiftUI tint propagation and is vulnerable to stale sheet
  presentation state after a recording attempt and re-entry.

## Architecture decisions

- Add the recent-turn collection to shared assistant state and refresh it after
  a completed answer is persisted. The Kotlin ViewModel remains responsible for
  conversation identity, ordering, retention, loading, and error state.
- Project history to an Objective-C-friendly iOS store state with primitive
  fields, including epoch milliseconds and grouped conversation projections,
  following the existing dictation-store bridge pattern. No Swift database or
  repository access is introduced.
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
AssistantStore iOS projection (turns, conversation ids + epoch dates)
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
3. Chat history shows up to the 15 persisted turns grouped into conversation
   blocks, supports local search, exposes every question/answer exchange on
   selection, and clearly marks cloud/on-device and partial responses.
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
| Existing data has turns but no conversation/session id | Unrelated answers could be merged or users could be misled about what can be reopened | Migrate legacy rows to one safe group per row and use stable ids for new chats |
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

## Current implementation slice: grounded breeding dates, resilient dictation, and new chats

### Confirmed requirements

- A breeding/gestation record's breeding date is authoritative record data and
  must answer questions such as "How long ago was this mare bred?" without
  depending on a small or unreliable model to notice a date in a retrieved card.
- Switching dictation language must keep the current sheet and transcript flow
  stable; changing the language only changes the speech/extraction engine
  configuration while the capture sheet is idle.
- iPhones without Apple Foundation Models still need a usable extraction path
  when Cloud AI is configured. The transcript must go through the shared
  Kotlin cloud boundary, not a second Swift networking implementation.
- The assistant needs an explicit new-chat action that clears only the visible
  conversation. Persisted recent-turn history remains available separately.

### Architecture decisions

- Add deterministic breeding-timing facts to the shared gestation projection,
  including breeding date and elapsed days, and answer that intent before any
  model call. Keep the existing live gestation-day calculation as the source of
  truth.
- Keep native SpeechAnalyzer/SFSpeechRecognizer and local Foundation Models in
  Swift. Add a Kotlin `GenerateDictationSessionUseCase` over the existing routed
  cloud engine for structured fallback; decode and normalize the JSON in Kotlin
  before Swift validation/review.
- Make language preparation an explicit cancellable idle-state operation and
  ignore stale engine results after a quick language switch. Swift may show the
  current source/status, but it must not own extraction or persistence logic.
- Expose `startNewChat()` through `AssistantStore` and `AssistantViewModel`;
  it clears in-memory messages only and never deletes the 15-turn database
  history.

### Acceptance criteria

1. English and Portuguese breeding-timing questions return the stored breeding
   date and elapsed days from Kotlin with a gestation source card; no cloud or
   local model can replace those facts.
2. Changing dictation language while idle does not dismiss/recreate the sheet,
   clear the transcript, or apply a stale engine for the previous language.
3. On an iPhone where Foundation Models are unavailable, Extract uses the
   configured cloud model, accepts common JSON/code-fence wrappers, and routes
   the result through the existing Kotlin validation/review flow. With no cloud
   configuration, it fails with an actionable message and preserves the text.
4. The Assistant toolbar exposes New chat; tapping it clears the visible turn
   list, leaves Chat history intact, and enables a fresh question immediately.
5. No API key is added to source, logs, fixtures, or tests; all shared logic
   remains in Kotlin and the final commit passes the repository hook.

### Verification plan

- Add focused common tests for breeding date/elapsed-day intent, Portuguese
  output, malformed-wrapper JSON normalization, cloud extraction failures, and
  new-chat state retention.
- Run focused desktop tests, `iosSimulatorArm64Test`, detekt, ktlint, and the
  iOS simulator/device native builds.
- Add/execute simulator UI coverage for switching dictation language in place,
  opening a fresh assistant chat, and keeping history accessible afterward.
- On device, confirm the extraction fallback's status/error path without
  recording or logging transcript contents beyond the visible review UI.

### Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| A cloud model wraps JSON in prose or markdown | Extraction appears broken despite valid records | Kotlin extracts the JSON envelope, decodes the DTO, and rejects malformed output before review |
| Cloud extraction sends clinical transcript text off-device | Privacy surprise | Only use the fallback when Cloud AI is already enabled/configured and show a clear cloud-source notice |
| Rapid language changes race async speech asset resolution | Wrong-language recognition or stuck loading state | Cancel prior preparation and apply a result only when its language still matches the selection |
| New chat accidentally clears retained history | User loses useful prior answers | Reset only `messages`; leave the history use case/repository untouched and test both paths |

### Implementation outcome

- Breeding timing is now answered deterministically from the breeding card's
  stored date, including elapsed days and a matching source card; model output
  cannot replace those facts.
- iPhone dictation keeps the sheet stable while switching languages. When Apple
  Foundation Models are unavailable and Cloud AI is configured, structured
  extraction uses the shared Kotlin cloud route and remains reviewable before
  saving; otherwise the transcript is preserved with an actionable message.
- The assistant now exposes a New chat action that clears only the visible
  conversation while keeping recent history available.
- Shared iOS tests, ktlint, and detekt pass. Simulator UI coverage exercises
  language switching, new-chat reset, and the full deterministic dictation
  review/archive flow. The committed device build also built, installed, and
  launched successfully on Daniela's paired iPhone.

## Current implementation slice: durable dictation audio, safer LLM routing, and compact iOS UI

### Behavior that must become true

- A completed dictation with microphone frames produces a validated local audio
  file, persists its metadata through Kotlin, and can play, stop, finish, and
  play again after archive re-entry or app relaunch.
- Playback configures and releases the native audio session explicitly; a stale
  record-only session must never make an otherwise valid CAF appear broken.
- Empty/whitespace Foundation Models output is not a successful answer and must
  fall back to the configured cloud route. A cloud `finish_reason=length` is an
  interrupted response, even when partial text exists.
- Cloud general/educational questions remain usable when active patients exist;
  title-cased veterinary terms must not be mistaken for unknown patient names.
- Dictation suggestions must match their declared record type and shared Kotlin
  must re-check patient resolution at the insertion boundary.
- Patient-detail navigation, assistant follow-ups, and record rows remain usable
  on compact iPhones and at larger text sizes.

### Behavior that must remain true

- Speech recognition and AVFoundation stay native; extraction, validation,
  patient resolution, persistence, RAG policy, and safety rules stay in Kotlin.
- English/Portuguese switching keeps the dictation sheet stable, original
  transcripts remain available when extraction fails, and cloud models never
  invent missing patient facts or dosage records.
- Audio and chat history remain device-local and existing database migrations,
  backups, record insertion, source cards, themes, and deletion paths continue
  to work.

### Ordered slices and verification

1. Audio lifecycle: add a native playback controller with explicit session
   activation/deactivation, validate written CAFs before persisting paths, and
   make the deterministic transcriber produce test audio. Verify with focused
   simulator UI playback/archive coverage plus a physical-device record/play
   pass when Daniela's iPhone reconnects.
2. LLM routing and safety: add failing tests for blank-primary fallback,
   output-limit interruption, title-cased educational questions, record-type
   payload compatibility, and insertion-boundary patient checks; make the
   smallest Kotlin fixes and rerun focused/shared iOS tests.
3. Compact UI: make follow-ups horizontally scrollable, replace the cramped
   five-way segmented patient picker with accessible scrollable controls, and
   allow clinical rows to wrap. Verify on the current simulator and with
   focused UI tests/screenshots.
4. Run ktlint/detekt, the full shared iOS suite, native simulator/device builds,
   and a separate Luna diff review. Fix confirmed review findings, commit, and
   install/launch the final build on Daniela's iPhone if available.

### Main failure modes

| Failure mode | Evidence/mitigation |
| --- | --- |
| CAF exists but playback is silent/fails | Recording leaves `AVAudioSession` in `.record`; playback owns an explicit `.playback` session and delegate lifecycle |
| Optional writer persists an empty/corrupt path | Require positive frames and a readable audio file before exposing the path |
| Small/local model emits blank text | Ignore blank primary snapshots and route to cloud only under the existing opt-in readiness policy |
| Provider reaches an output limit | Preserve partial text but emit the existing interrupted/retry state |
| General veterinary term looks like a patient name | Test title-cased educational prompts with active patients and narrow unknown-patient detection |
| Swift caller supplies an arbitrary patient id | Shared insertion boundary re-resolves or validates every accepted suggestion |
| Compact UI controls truncate or lose activation | Scroll/wrap controls and retain explicit accessibility labels/identifiers |

### Current limitation

- Daniela's iPhone was paired earlier, but CoreDevice lost the tunnel before the
  metadata-only `Documents/dictations` inspection. Physical microphone and
  audible playback remain unverified until the device reconnects; simulator and
  static evidence cannot prove microphone routing or speaker output.

### Implementation outcome

- Archived recordings now use a dedicated AVFoundation playback controller
  that temporarily owns a playback audio session, restores the prior session,
  and follows player delegate completion instead of a duration timer.
- Audio paths are persisted only for readable, non-empty recordings. The
  deterministic simulator transcriber now writes a valid CAF, so the complete
  save/archive/play/stop/play-again path is covered by UI automation.
- Kotlin now falls back from blank Foundation Models output, reports cloud
  output-limit responses as interrupted while preserving partial text, keeps
  educational questions out of accidental patient-name scoping, validates
  dictation payload types, and rejects invalid patient ids at insertion.
- Compact iPhone UI now uses scrollable patient tabs and assistant follow-ups,
  and clinical rows can wrap without squeezing their dates.
- The shared iOS suite, focused Android cloud tests, ktlint, detekt, native iOS
  simulator build, and deterministic dictation playback UI test pass. A Luna
  diff review found no blocking issues; its payload/session concerns were
  incorporated before the final verification pass.

## Current implementation slice: consistent dictation deletion and conversation blocks

### Behavior that must become true

- Dictation archive deletion keeps the existing full-swipe behavior but uses the
  same text-only red swipe affordance as patient, owner, and clinical-record
  rows.
- Assistant history persists a stable conversation id with each completed turn,
  groups turns by that id in Kotlin, and reopens only the most recent chat in
  the live transcript on app launch.
- Legacy assistant rows remain reviewable after migration and are never merged
  into one unrelated conversation. Backups preserve conversation ids while
  still accepting payloads created before this field existed.
- Chat history has a clear conversation list and a detail view containing every
  exchange, per-turn reuse actions, source labels, and partial-response state.

### Verification plan

- Run the shared conversation-grouping and assistant-retention tests on iOS and
  Android host targets, plus the iOS simulator framework compile.
- Run ktlint and detekt, then build the native iOS simulator app.
- Run focused simulator UI coverage for opening chat history and the existing
  deterministic dictation save/archive/playback path.
- Obtain a Luna diff review, commit the verified slice, and install/launch on
  Daniela's iPhone if its CoreDevice tunnel is available.

## Current implementation slice: SonarQube quality analysis

1. Add the pinned SonarScanner for Gradle at the root and keep analysis opt-in
   so ordinary Android/KMP builds do not require a Sonar server.
2. Give Sonar an explicit KMP/iOS source and test scope, exclude generated and
   resource-container noise, and import the existing Detekt, KtLint, Android
   Lint, JUnit, and Kover reports.
3. Document a strict new-code quality-gate policy that blocks regressions while
   keeping existing debt visible, and keep all credentials outside the repo.
4. Verify report generation and the Sonar task graph, review the diff, and
   commit the configuration.

### Verification boundary

- Static analysis and Android Lint report generation pass.
- `sonar --dry-run` validates task wiring without requiring a server token.
- Full coverage generation remains subject to the two pre-existing shared test
  failures recorded by the existing baseline; the Sonar configuration does not
  suppress or exclude those tests.
