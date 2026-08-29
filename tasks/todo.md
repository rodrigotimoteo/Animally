# Task checklist: iOS-first product polish

- [x] Audit current theme/settings, Patients/Owners, dictation, and assistant paths
- [x] Add persisted accent-colour selection and wire all theme consumers
- [x] Improve iOS Patients and Owners list/detail UX
- [x] Add deterministic simulator dictation coverage and document device-only limits
- [x] Improve assistant reliability, output normalization, and human tone
- [x] Run focused tests, broad checks, and iOS simulator smoke tests
- [x] Build the final device binary and commit the completed change
- [x] Reinstall and launch on Daniela's iPhone once its CoreDevice tunnel is available
- [x] Confirm a clean worktree

## Current slice: dictation theme and assistant history UX

- [x] Reproduce and fix the stale accent tint across dictation sheet re-entry
- [x] Expose Kotlin-owned recent turns through the iOS assistant store
- [x] Add searchable chat-history list/detail UI with a safe “Use question” action
- [x] Add/extend focused Kotlin and iOS UI coverage
- [x] Run verification, review the diff, and commit the slice

## Current slice: synthetic herd fixture and cloud evaluation

- [x] Add a separate fictional schema-v1 demo herd backup without personal data
- [ ] Validate and restore the fixture through the iOS Settings flow (payload pasted; final button tap was not confirmed by simulator automation)
- [x] Exercise cloud retrieval, analysis tools, source cards, refusals, and bilingual output (bounded run; the supplied OpenRouter key then returned HTTP 403 key-limit-exceeded)
- [x] Review failures, run focused checks, and commit the slice

## Current slice: breeding dates, iPhone extraction, and new assistant chats

- [x] Reproduce the missing breeding-date answer against the synthetic herd
- [x] Add deterministic breeding-date/elapsed-day answers and tests
- [x] Add Kotlin-owned cloud structured-extraction fallback for iPhone
- [x] Stabilize dictation language switching and add UI coverage
- [x] Add visible new-chat action without deleting retained history
- [x] Run checks/builds, install the final device build, and commit

## Current slice: durable audio, LLM safety, and compact iOS UI

- [x] Run parallel Luna audits against the active branch
- [x] Reproduce and fix dictation audio file/playback lifecycle
- [x] Add deterministic simulator audio playback/archive coverage
- [x] Harden blank-output fallback and cloud output-limit handling
- [x] Keep title-cased educational cloud questions out of patient scoping
- [x] Enforce record-type payload and patient-id validation in Kotlin
- [x] Fix compact patient tabs, assistant follow-ups, and record-row wrapping
- [x] Run focused/full checks and obtain a Luna diff review
- [x] Commit the verified final slice
- [ ] Install/launch on Daniela's iPhone when its CoreDevice tunnel is available

## Current slice: consistent dictation deletion and conversation blocks

- [x] Match the dictation archive swipe label and red tint to other delete rows while retaining full-swipe behavior
- [x] Persist stable assistant conversation ids through SQLDelight and backups
- [x] Group retained turns in Kotlin and expose grouped iOS store projections
- [x] Add searchable conversation list/detail UI with per-turn question reuse and source/partial markers
- [x] Capture the conversation id at request start to prevent in-flight reassignment
- [x] Run focused shared tests, native simulator build, and assistant/dictation UI smoke tests
- [x] Run the final full verification pass and commit the slice
- [ ] Install/launch on Daniela's iPhone when its CoreDevice tunnel is available

## Current slice: SonarQube quality analysis

- [x] Pin and apply the root SonarScanner for Gradle plugin
- [x] Scope shared KMP, Android, iOS host, and test sources explicitly
- [x] Import Detekt, KtLint, Android Lint, JUnit, and Kover reports
- [x] Document a strict, new-code-focused quality-gate policy and secret handling
- [x] Verify static-analysis reports and the Sonar task graph
- [x] Run a real local SonarQube scan and resolve source/report integration issues
- [x] Configure the documented gate on the local server while keeping credentials external
- [x] Resolve the two pre-existing shared test failures so the full coverage/Sonar run can complete
- [x] Commit the verified configuration

## Current slice: shared regressions and all Sonar findings

- [x] Fix the accented Portuguese educational-question regression
- [x] Confirm the RAG golden-set contract and fix retrieval or stale expectations
- [x] Resolve deterministic Kotlin, Android Lint, and platform-parameter findings
- [x] Refactor the three reported high-complexity LLM methods without changing behavior
- [x] Run full coverage, shared tests, static checks, and a fresh local Sonar gate
- [x] Review the diff, commit the fixes, and confirm a clean worktree

## Current slice: dictation stability, edited transcripts, and playback

### Phase 1: theme lifecycle and startup stability

- [x] Reproduce the Assistant/Dictation accent mismatch and language/startup flicker
- [x] Remove visible preparation flicker while preserving readiness and errors
- [x] Apply the concrete selected accent to Assistant, Dictation review, archive, and audio controls
- [x] Add focused language-switch/re-entry accent UI coverage

### Phase 2: authoritative edited transcript

- [x] Add a Kotlin-owned update-by-capture-id repository operation/use case
- [x] Bridge capture id and update the saved transcript before extraction
- [x] Ensure extraction and archive receive the same normalized editor value
- [x] Add regression coverage for edited text and extraction failure/retry

### Checkpoint A

- [x] Focused shared tests and KMP iOS build pass
- [x] Deterministic simulator flow shows edited transcript in history
- [x] Review diff for Swift/Kotlin separation before continuing

### Phase 3: structured extraction reliability

- [x] Add provider-shape fixtures for thinking blocks, wrappers, cumulative output, and terminal frames
- [x] Distinguish valid empty records from invalid/blank extraction output
- [x] Preserve transcript/audio and expose a useful retry path for failed extraction
- [x] Verify English and Portuguese extraction contracts

### Phase 4: playback controls

- [x] Add published playback progress, duration, rate, and seeking
- [x] Add progress slider, elapsed/remaining time, and 1x/1.5x/2x controls
- [x] Verify stop/completion/missing-file/dismiss/replay cleanup

### Final verification

- [x] Run focused and full shared tests, ktlint, detekt, and native simulator build/UI tests
- [x] Review the complete diff for architecture, style, and regressions
- [x] Commit the complete slice and confirm a clean worktree

## Current slice: cloud assistant quality and grounded analysis

- [x] Audit and document cloud/general-vs-record routing contracts
- [x] Improve intent classification without weakening patient-fact grounding
- [x] Extend deterministic Kotlin analysis summaries/tools and empty-data behavior
- [x] Make cloud answers useful, cautious, and naturally conversational
- [x] Add a 100+ case adversarial assistant contract matrix
- [x] Exercise compatible free cloud models and provider fallbacks without leaking keys
- [x] Run shared/native/simulator verification and review the diff
- [x] Resolve breeding timing from the reproduction card before gestation fallback
- [x] Commit the complete slice and confirm a clean worktree
