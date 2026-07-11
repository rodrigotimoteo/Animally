# ADR 0014: Dual Backup Formats — JSON and Raw DB

**Date**: 2026-07-11
**Status**: proposed

## Context
Phase 3 backup/restore needs to handle DB + file attachments. Raw DB copy is fast but not portable across schema versions. JSON export is portable but slower.

## Decision
Support both formats. JSON as default backup (all entities serialized via kotlinx.serialization, schema-versioned, portable, human-readable). Raw DB + attachments folder as "fast backup" option (instant, but only restorable on same schema version).

## Alternatives considered
1. Raw DB only — fastest, but not portable across schema versions without migration.
2. JSON only — portable, human-readable, but slower for large datasets, must re-insert all rows on restore.
3. CSV — spreadsheet-friendly but loses relationships, not suitable for full backup.

## Consequences
Easier: JSON for portability + cross-version restore, raw DB for speed.
Harder: two backup code paths, JSON schema versioning needed, user must choose format.
