# Dictation and grounded assistant checklist

- [x] Add shared date-range intent parsing and date-aware record retrieval.
- [x] Enforce explicit patient scope before context selection and source cards.
- [x] Add deterministic recent-activity output and strict cloud record gating.
- [x] Tighten prompt data/grounding instructions without removing general cloud
      questions.
- [x] Add English/Portuguese dictation selection and pass it through the iOS
      speech resolver.
- [x] Harden AVAudioSession/analyzer format setup and legacy fallback.
- [x] Add focused shared regressions for every reported RAG failure mode.
- [x] Run shared tests/lint, iOS build, and simulator UI coverage.
- [x] Review diff, commit the completed changes, and report device-only limits.
