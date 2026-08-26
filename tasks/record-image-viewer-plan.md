# Implementation Plan: iOS record image viewer

## Overview

Make images attached to imaging and ultrasound records useful on iOS. The
existing persistence already stores comma-separated app-local paths, but the
read-only detail bridge currently emits only text rows and the iOS edit field
cannot render more than one path. The change will carry typed attachment
metadata from Kotlin to Swift and provide a reusable, accessible gallery for
editing and read-only record details.

## Architecture decisions

- Keep attachment ownership and comma-separated path parsing in shared Kotlin;
  Swift receives typed attachment metadata through `RecordDetailState`.
- Keep file decoding and image presentation in SwiftUI/UIKit, because those
  are iOS presentation concerns and do not belong in the database or record
  view models.
- Reuse one gallery/viewer implementation for edit previews and read-only
  details so paging, zooming, missing-file feedback, and accessibility do not
  drift between screens.
- Do not change the persisted schema or attachment file format; existing
  records and backups remain compatible.

## Acceptance criteria

- An imaging or ultrasound detail state includes every non-blank persisted
  image path, and ordinary records remain unchanged.
- The iOS read-only detail shows a labeled attachment section when images are
  present, with thumbnails, full-screen paging, pinch-to-zoom, and a clear
  missing/unreadable-file state.
- The iOS edit forms preview all attached images, allow adding more images,
  and remove only the selected image while preserving the other paths.
- Empty attachments do not add an empty section or destabilize loading/error
  states.
- Existing shared attachment/view-model tests, iOS compilation, and focused
  UI smoke coverage pass; the feature is manually checked on the simulator.

## Work items

### Phase 1: Kotlin contract

- [x] Add an Objective-C-visible attachment model and attach it to
  `RecordDetailState`.
- [x] Decode imaging and ultrasound form paths in Kotlin and pass them through
  the existing typed detail opener.
- [x] Add focused tests for path splitting/metadata and detail-state behavior
  where the existing test harness permits.

### Phase 2: iOS presentation

- [x] Build a reusable local-image gallery/viewer with loading, failure,
  paging, zoom, and accessibility states.
- [x] Render attachment galleries in read-only record details.
- [x] Update the edit attachment field to handle multiple paths and keep its
  add/remove behavior consistent with the shared view-model contract.

### Checkpoint: verification

- [x] Focused Kotlin tests and static analysis pass.
- [x] Simulator build and focused UI/manual detail-flow check pass.
- [x] Working tree is clean after the commit.

## Main failure modes and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Stored path is a `file://` URL or a legacy absolute path | Valid image appears missing | Normalize both forms at the Swift presentation boundary and test both forms in the path contract. |
| Several paths are stored in one field | Only one image appears or removal corrupts the list | Split and join paths in Kotlin state/view-model code; identify each gallery item by its full path. |
| Large or deleted local file | UI jank or blank preview | Decode off the main actor and render explicit loading/error states. |
| Detail state reloads after editing | Stale or duplicated images | Derive attachments from every emitted Kotlin state and replace the published list atomically. |

## Verification plan

- Run the narrow shared attachment/detail tests and `detekt`/`ktlintCheck`.
- Build the iOS simulator target with the generated Kotlin framework.
- Run a focused UI test or manual simulator flow that opens an imaging or
  ultrasound detail and opens the image viewer; inspect screenshots for the
  attachment section, paging, and zoom affordance.
- Report physical-device validation separately if a device is unavailable.
