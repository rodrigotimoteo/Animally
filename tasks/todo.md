# Maintenance Quality Sweep Checklist — 2026-09-07

## Current sweep

- [x] Baseline current Gradle, iOS host, script, and simulator/UI verification.
- [ ] Build the feature/route/test coverage matrix from current source.
- [x] Audit assistant cancellation, streaming, retries, view lifecycle, and source traceability.
- [x] Audit dictation capture/transcription/extraction/review/save/playback and edge cases.
- [ ] Add or repair deterministic iOS UI test seeding/reset and remove paused tests.
- [ ] Add migration upgrade fixtures and recovery-contract tests.
- [ ] Verify restore/media/wipe/privacy behavior without guessing unresolved product policy.
- [x] Verify reminder scheduling and cancellation across lifecycle/permission changes.
- [ ] Verify Android route reachability, permission prompts, sharing, and compact settings.
- [x] Verify record-family parity across sync, search, timeline, export, backup, and native routing for the repaired slices.
- [x] Add golden edge-case fixtures and source-traceability assertions for the repaired slices.
- [ ] Add one offline release-confidence aggregate gate and update documentation.
- [x] Perform deletion/simplification pass, review actual diff, run final matrix, and commit.

## Evidence required before completion

- [x] Focused regression test for every behavior change.
- [x] `:shared:testAndroidHostTest`, `:shared:desktopTest`, and `:shared:iosSimulatorArm64Test` pass.
- [x] `:shared:ktlintCheck`, `:shared:detekt`, `:shared:koverVerify`, Android lint/package, and iOS host build pass.
- [x] Script contracts and the simulator helper pass; live UI evidence is recorded, with UI-test isolation explicitly left open.
- [x] No credentials, unresolved destructive-policy changes, or unexplained broad catches/detached tasks are introduced.

---

# Internship Insights Dashboard Checklist

## Foundation

- [ ] Canonicalise reproduction event types and legacy values.
- [ ] Define shared insights filters, snapshots, metrics, comparisons, and drill-down contracts.
- [ ] Implement SQLDelight overview/activity/record-mix aggregates.
- [ ] Add deterministic repository integration fixtures.

## Overview MVP

- [ ] Build the shared insights use case and ViewModel.
- [ ] Add date presets, patient scope, and safe previous-period comparison.
- [ ] Render the iOS overview and case-mix charts.
- [ ] Add Timeline and Patient Detail entry points without adding a sixth root tab.
- [ ] Make every metric drill down to source records.

## Reproduction

- [ ] Add event, embryo collection, ICSI, and ultrasound metrics.
- [x] Add current active-gestation and due-soon snapshots.
- [ ] Verify no unsupported success rate is shown.

## Thesis Readiness

- [ ] Export dashboard-aligned CSV datasets and a data dictionary.
- [ ] Add stable pseudonymous patient IDs and privacy regression tests.
- [ ] Add explicit research-readiness issue counts and drill-down.

## Verification

- [ ] Dashboard and export totals match golden fixtures.
- [ ] Empty, zero-denominator, inactive-row, and unknown-category cases pass.
- [ ] Light, dark, system theme, Dynamic Type, and VoiceOver checks pass.
- [ ] Shared Android-host/iOS tests, Detekt, KtLint, and Xcode build pass.
- [ ] Install on a physical iPhone for workflow verification.
- [ ] Commit the completed implementation batch.

## Follow-up UI polish

- [x] Make playback-speed sheet labels readable in both appearances.
- [x] Pass the selected accent into patient-history PDF rendering.
- [x] Use a neutral “Activity overview” heading and equal-height stat cards.
- [x] Let research-readiness copy wrap without crowding the count or chevron.
- [x] Run focused shared tests, iOS compilation/build checks, and `git diff --check`.

## Current request: dictation and medical reference follow-up

- [x] Add bounded typo-tolerant canonicalisation for the supported medical reference vocabulary.
- [x] Add medication to the dictation DTO, validator, insertion path, and review projection.
- [x] Resolve unique patient-name prefixes while preserving ambiguity for collisions.
- [x] Show the finalized transcript during extraction and tighten the dictation review layout.
- [x] Run focused shared tests, lint/detekt, iOS build, and reinstall the verified build on Rodrigo’s iPhone.

## Current request: generated trusted medical vocabulary

- [x] Add the NLM MeSH vocabulary update script and generated disease/drug index.
- [x] Use generated terms/aliases in the privacy-safe reference matcher.
- [x] Verify leishmaniasis and trusted-source card regressions, lint, compilation, and iPhone install.
