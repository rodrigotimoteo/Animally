# Assistant and dictation retention

## Goal

Persist a useful recent assistant conversation and preserve each completed iOS
dictation as both editable text and its original audio recording. The feature
must keep data and retention policy in shared Kotlin while leaving microphone
and audio-file capture in the iOS adapter.

## Decisions

- Retain the latest 15 completed assistant question/answer turns. A turn is a
  user question paired with the assistant response, including an interrupted
  response when some text was produced.
- Keep dictation captures independently of the 15-turn chat limit. A capture
  is written when recording stops, before extraction or suggestion review.
- Store transcript, capture time, duration, and an app-local audio path in the
  database. Store audio bytes in the existing app-private Documents storage;
  do not put large audio blobs in SQLite.
- Keep the feature local to the device. Chat text and raw audio are sensitive
  and are not added to CloudKit synchronization in this iteration.
- The iOS speech adapter may fail to produce an audio file without blocking
  transcription. The transcript is still retained and the UI reports when
  playback is unavailable.
- SwiftUI may own AVFoundation playback and the audio-file bridge, but all
  database writes, retention, ordering, and history state remain in shared
  Kotlin.

## Acceptance criteria

1. Relaunching the app restores up to 15 recent assistant turns in chronological
   order and sends the persisted turns through the existing RAG history seam.
2. Saving a 16th completed turn removes only the oldest turn; interrupted and
   blank/failed turns never create empty chat bubbles or corrupt ordering.
3. Stopping dictation stores the transcript even when structured extraction
   fails or the user cancels review later.
4. When iOS audio capture succeeds, the original recording remains playable
   after the dictation sheet closes and after a subsequent app launch.
5. Missing/unreadable audio never prevents transcript/history display.
6. The assistant screen exposes recent chat history naturally, and dictation
   history provides search, date, transcript preview, and playback when the
   file exists.
7. Unit tests cover 15-turn retention, ordering, transcript persistence, and
   missing-audio behavior; iOS UI tests cover the deterministic dictation path.
8. Shared Kotlin tests, lint/detekt, iOS simulator build, and the focused iOS
   assistant/dictation UI test pass before committing.

## Implementation order

1. Add SQLDelight tables, migration, repository contracts/implementations, and
   Koin bindings.
2. Add shared assistant/dictation ViewModel state and store APIs, including
   bounded history hydration and capture persistence.
3. Extend the iOS speech adapter to record a CAF file alongside recognition,
   retaining completed files and deleting only aborted partial files.
4. Add SwiftUI history/archive presentation and playback affordances.
5. Add focused tests, run verification, inspect simulator behavior, and commit.
