# Dictation languages and grounded assistant

## Goal

Make iOS dictation usable in English and Portuguese and make record answers
authoritative: patient-specific claims must come from active application data,
never from broad retrieval matches, conversation text, or cloud-model memory.

## Confirmed failure modes

- The iOS 26 SpeechAnalyzer path chooses a preferred locale but has no explicit
  English/Portuguese choice. Its audio session errors are swallowed, the input
  route/format is not stabilized before converter creation, and a conversion
  failure only reports an error instead of falling back to the legacy engine.
- The RAG query `What happened this month?` has no date-range interpretation.
  `this`, `month`, and `happened` reach the broad OR retry, so unrelated rows
  can become context. Cloud policy then permits generation even when record
  context is empty or irrelevant.
- Patient matching currently reorders matching rows but leaves other patients
  in the prompt. Citation enforcement proves only that a row was retrieved; it
  does not prove the model's prose is supported by that row.

## Architecture decisions

- Keep language choice, date interpretation, patient scope, retrieval filters,
  and factual refusal rules in shared Kotlin. Swift owns only the picker UI and
  AVFoundation/Speech platform adapter.
- Add a date-aware retrieval seam backed by the existing search-index metadata
  so date-only activity questions enumerate actual dated active records rather
  than searching filler words.
- Treat explicit record questions as strict on every provider, including cloud.
  Cloud general-knowledge flexibility remains available only for clearly
  non-record questions.
- Filter explicit patient scope before budgeting/context construction. A
  matching patient is an exclusion boundary, not merely a ranking boost.
- Answer the simple recent-activity shape deterministically from retrieved
  rows. This avoids asking a model to invent a narrative for a question whose
  answer is already in the database.
- Use the selected language for both iOS SpeechAnalyzer and legacy
  `SFSpeechRecognizer`, with a device-safe fallback and a clear language hint.
- Configure and validate the audio session before reading the input format;
  if the preferred analyzer cannot start, retry the same language through the
  legacy recognizer without losing the capture flow.

## Acceptance criteria

1. A user can select English or Portuguese in the dictation sheet and the
   selected language is passed to the selected speech engine.
2. A valid microphone route does not produce the current “could not prepare the
   microphone audio” error solely because the hardware and analyzer formats
   differ; the app either converts safely or falls back to legacy recognition.
3. Existing transcript and original-audio retention remains intact on success,
   cancellation, and recognition failure.
4. `What happened this month?` uses only active dated records from the current
   month through today, and no record outside that range can reach the model or
   deterministic answer.
5. An explicit patient name scopes retrieval to that patient; rows belonging to
   another patient cannot be used as context or source chips.
6. Cloud routing does not bypass the no-records refusal for record questions,
   while clearly general questions still reach a cloud model.
7. The assistant marks retrieved content as data, preserves warm/plain-language
   guidance, and never turns a model-invented citation into a source card.
8. Focused shared tests cover date ranges, date-only activity, patient
   exclusion, cloud record gating, and general-question compatibility.
9. iOS builds cleanly; simulator coverage exercises the deterministic dictation
   route and the real microphone limitation is reported separately.

## Verification and risks

- Run the focused RAG/common tests first, then the full shared test/lint tasks
  that the repository supports.
- Build the iOS simulator target and run the deterministic dictation UI test.
  A simulator cannot establish real microphone routing or installed speech
  assets; if a physical device is unavailable, that remains an explicit risk.
- Inspect the final diff for Kotlin/Swift boundary violations and run the
  existing regression suite before committing.
- A provider can still ignore a prompt. The hard protection is therefore the
  Kotlin record gate, patient/date filtering, deterministic activity answer,
  and honest refusal—not a claim that arbitrary cloud prose can be mathematically
  fact-checked after generation.
