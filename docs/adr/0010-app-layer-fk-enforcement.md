# ADR 0010: App-Layer Foreign Key Enforcement Over DB Cascade

**Date**: 2026-07-11
**Status**: proposed

## Context
SQLDelight doesn't enforce foreign keys by default. Deleting an Owner with Patients, or a Patient with 18+ record types, needs a strategy. DB cascade is automatic but dangerous. Soft delete preserves history but clutters queries.

## Decision
App-layer enforcement — repos check dependencies before delete. Block deletion if child records exist. Return error to UI. Patient has isActive flag for soft-delete (deactivate, don't delete). Owner deletion blocked if active patients exist.

## Alternatives considered
1. PRAGMA foreign_keys = ON + ON DELETE CASCADE — DB enforces, automatic, but dangerous (one delete cascades to hundreds of records).
2. Soft delete everything — isActive on all entities, never hard-delete. Safe but queries need WHERE isActive = 1 everywhere, data grows indefinitely.
3. DB cascade with confirmation — cascade but require UI confirmation. Still risky.

## Consequences
Easier: explicit control, no accidental cascade, preserves history via soft-delete on Patient.
Harder: more code in repos (dependency checks), no DB-level integrity guarantee, manual enforcement per entity.
