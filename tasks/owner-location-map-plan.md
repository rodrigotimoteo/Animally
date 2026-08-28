# Implementation Plan: Shared owner locations with iOS map presentation

## Overview

Add an optional geographic location to each owner. Shared Kotlin owns the
location model, validation, persistence, backup, sync, and owner form state.
The iOS host provides a thin MapKit picker, preview, and Apple Maps handoff;
it does not perform database or business-logic work.

## Architecture decisions

- Keep the existing free-text address unchanged and add an optional validated
  latitude/longitude pair.
- Store the coordinates in the shared Owner table and include them in backup
  and CloudKit sync payloads, with null defaults for older data.
- Keep coordinate validation and normalization in shared Kotlin. The iOS
  adapter receives primitive coordinates only at the presentation boundary.
- Use MapKit's native map/search UI on iOS. Selecting a location never requires
  location permission; the user searches or moves the map and confirms the
  center pin.
- Make the map preview optional and openable in Apple Maps. Owners without
  coordinates continue to show their existing address normally.

## Task list

### Phase 1: Shared data contract

- [x] Add a validated shared owner-location value object and optional owner/form
  state.
- [x] Add nullable database columns and a forward migration without changing
  existing insert call sites unnecessarily.
- [x] Persist, map, back up, restore, and sync coordinates.

### Phase 2: iOS presentation

- [x] Expose a Kotlin-owned location update action through the iOS owner store.
- [x] Add a MapKit search/center-pin picker to the owner edit form.
- [x] Add a compact detail preview and an “Open in Maps” action.

### Phase 3: Verification and handoff

- [x] Add shared tests for validation, repository round-trip, backup, sync, and
  owner form propagation.
- [x] Build the shared iOS arm64 and iOS simulator targets and run focused
  tests/lint.
- [ ] Install the verified build on Daniela's paired iPhone when it is
  available and commit the
  completed feature.

## Main failure modes and mitigations

| Risk | Mitigation |
| --- | --- |
| Existing owners have only text addresses | Keep address nullable and coordinates optional; no migration data is required. |
| One coordinate is missing or invalid | Normalize incomplete/invalid pairs to no saved location in shared Kotlin. |
| MapKit/network search is unavailable | Let the user pan/zoom and confirm; saved coordinates remain usable offline. |
| iOS UI starts owning persistence | Swift only calls the store's location action; the Kotlin view model creates the domain owner. |
| Backup/sync drops coordinates | Add round-trip and payload contract tests with old-payload defaults. |
