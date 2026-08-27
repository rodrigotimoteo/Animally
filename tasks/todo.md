# Task checklist: iOS-first product polish

- [x] Audit current theme/settings, Patients/Owners, dictation, and assistant paths
- [x] Add persisted accent-colour selection and wire all theme consumers
- [x] Improve iOS Patients and Owners list/detail UX
- [x] Add deterministic simulator dictation coverage and document device-only limits
- [x] Improve assistant reliability, output normalization, and human tone
- [x] Run focused tests, broad checks, and iOS simulator smoke tests
- [x] Build the final device binary and commit the completed change
- [ ] Reinstall and launch on Daniela's iPhone once its CoreDevice tunnel is available
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
