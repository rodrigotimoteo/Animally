# ADR 0011: FTS5 Patient-Centric Search

**Date**: 2026-07-11
**Status**: proposed

## Context
App needs global search across all records — patient names, diagnoses, treatments, notes. FTS5 available in SQLDelight. Search scope spans 18+ entity types.

## Decision
Single search_fts virtual table joining Patient (name, breed, microchip, UELN) + Consultation (assessment, plan, subjective, objective) + all text fields from other entities. One query returns patient matches with highlighted context. Results grouped by Patient, expandable to show matching records.

## Alternatives considered
1. Per-entity FTS — separate FTS table per entity. More precise filtering but complex query orchestration, more tables.
2. Patient + Consultation only — simplest useful search but misses medications, lab results, etc.
3. LIKE queries — no FTS, simple but slow on large datasets, no ranking.

## Consequences
Easier: single search query, patient-grouped results, FTS5 ranking.
Harder: FTS table needs sync when source tables change (triggers or app-layer updates), larger FTS index, no per-entity filtering without extra columns.
