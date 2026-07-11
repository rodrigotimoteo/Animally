# ADR 0015: Phase 1 Split into 1a (Core) and 1b (Remaining Entities)

**Date**: 2026-07-11
**Status**: proposed

## Context
Phase 1 as written includes 18+ entities, each needing domain model + repository + .sq + ViewModel + screen + route = ~90+ files. Single pass risks incomplete entities and burnout. Timeframe is flexible (personal project) but deliverable quality matters.

## Decision
Split Phase 1. Phase 1a: Patient + Owner + Consultation + Vaccination (core workflow — see patient, add consultation, track vaccination). Phase 1b: remaining 14 entities (Deworming, Dentistry, Lameness, Surgery, Medication, Lab Result, Imaging, Farrier, Reproduction Event, Ultrasound, Gestation, Repro Med, Controlled Substance, Anamnese, Weight Entry). 1a proves architecture end-to-end. 1b batch-adds entities following established patterns.

## Alternatives considered
1. Full Phase 1 in one pass — complete but risk of incomplete entities, no deliverable until all done.
2. Vertical slice (Patient + Owner + one record type) — proves architecture but less useful as standalone.
3. All entities but no tests — faster but no quality gate.

## Consequences
Easier: 1a is deliverable and testable, architecture proven before scaling, 1b is mechanical pattern-following.
Harder: two sub-phases, some refactoring possible when 1b entities reveal pattern gaps.
