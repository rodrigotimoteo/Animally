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
- [ ] Add current active-gestation and due-soon snapshots.
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
