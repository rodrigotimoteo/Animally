# ADR 0005: Auto-Increment IDs Over UUID (Defer Sync Complexity)

**Date**: 2026-07-11
**Status**: proposed

## Context
Phase 1-5 is offline-first, single device. Phase 6 introduces cloud sync where ID conflicts matter. UUID prevents conflicts but adds complexity now.

## Decision
Use auto-increment INTEGER PRIMARY KEY for all tables. Defer UUID/sync-safe IDs to Phase 6. Migration will add UUID column + backfill when sync is implemented.

## Alternatives considered
1. UUID text PK from day 1 — sync-safe, no migration later, but larger index, string comparisons, client-side generation complexity.
2. Snowflake/ULID — time-ordered unique IDs, but external dependency.
3. Auto-increment + UUID dual column — best of both but redundant.

## Consequences
Easier: simple, fast, small index, DB-native.
Harder: Phase 6 sync requires ID migration (add UUID, backfill, update FK references). Risk: migration complexity if data set is large by then.
