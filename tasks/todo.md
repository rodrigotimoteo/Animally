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
