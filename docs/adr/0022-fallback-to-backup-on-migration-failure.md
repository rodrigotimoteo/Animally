# ADR 0022: Fallback to Backup on Migration Failure

**Date**: 2026-07-11
**Status**: proposed

## Context
DB schema migration can fail on app update (buggy migration, schema drift, corrupted DB). App stores medical records — data loss is unacceptable. Need graceful recovery path.

## Decision
On migration failure, attempt restore from last automatic backup (if exists). If no backup, offer "export raw data" before wiping DB. Log full error. Never silently drop DB.

## Alternatives considered
1. Crash + log — app refuses to start, data safe (old DB intact), but user stuck until developer fixes migration.
2. Silent recreate — drop DB, start fresh. User loses all data. Unacceptable for medical records.
3. Retry migration — may loop forever if migration is fundamentally broken.

## Consequences
Easier: graceful recovery, data preservation, user has options.
Harder: requires automatic backup system (Phase 3 dependency), complex error handling, "export raw data" needs implementation. Risk: if no backup exists and migration fails, user is stuck.
