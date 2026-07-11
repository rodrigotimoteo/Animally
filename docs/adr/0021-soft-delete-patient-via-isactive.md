# ADR 0021: Soft Delete Patient via isActive Flag

**Date**: 2026-07-11
**Status**: proposed

## Context
Patient is central entity — all records link to it. Hard-deleting a Patient would orphan 18+ record types. Medical records should be preserved even if patient dies or is transferred.

## Decision
Patient has isActive: Boolean field (already in model). Deactivate patients, never hard-delete. Inactive patients hidden from default list but accessible via filter. All child records preserved.

## Alternatives considered
1. Hard delete with cascade — automatic but destroys medical history, unacceptable for medical records.
2. Hard delete with app-layer check (block if records exist) — safe but patient can never be removed once it has records.
3. Archive table — move inactive patients to separate table. Complex, breaks FK references.

## Consequences
Easier: medical history preserved, reversible (reactivate patient), simple flag.
Harder: queries need WHERE isActive = 1 by default, inactive patients still in DB (storage), UI needs filter toggle.
