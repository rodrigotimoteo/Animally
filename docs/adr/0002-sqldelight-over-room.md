# ADR 0002: SQLDelight Over Room for Local Database

**Date**: 2026-07-11
**Status**: proposed

## Context
App needs local SQLite database with KMP support (Android + iOS). Room is Android-only. SQLDelight is KMP-native with FTS5 support needed for Phase 2 search.

## Decision
Use SQLDelight 2.3.2. Schema defined in .sq files, type-safe generated queries. Platform-specific drivers via expect/actual (AndroidSqliteDriver, NativeSqliteDriver).

## Alternatives considered
1. Room — Android-only, no iOS support. Would need separate DB layer per platform.
2. Realm — KMP support but different paradigm (object DB), less SQL control.
3. Raw SQLite via expect/actual — no type safety, manual query mapping.

## Consequences
Easier: single schema, type-safe queries, FTS5 support, KMP-native.
Harder: .sq file learning curve, no ORM convenience (manual mappers), migration files required.
