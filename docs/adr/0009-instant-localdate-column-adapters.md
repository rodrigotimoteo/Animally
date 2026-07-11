# ADR 0009: Instant to Long, LocalDate to String Column Adapters

**Date**: 2026-07-11
**Status**: proposed

## Context
SQLDelight stores primitive types (Long, String, Boolean). Domain models use kotlinx.datetime Instant and LocalDate. Need column adapters that preserve type safety and query efficiency.

## Decision
Instant → Long (epoch millis) via InstantColumnAdapter. LocalDate → String (ISO 8601 "yyyy-MM-dd") via custom adapter. Use sqldelight-primitive-adapter for Instant, hand-rolled adapter for LocalDate. ISO strings sort lexicographically for date range queries.

## Alternatives considered
1. Both as String — simplest, one adapter type, but Instant as ISO string verbose, slower timestamp comparisons.
2. Both as Long — fastest queries but LocalDate as epoch-day opaque, can't eyeball DB rows, overkill for app's data volume.
3. Store as text always, parse on read — no adapters but no type safety at query level.

## Consequences
Easier: fast timestamp sort/filter, human-readable dates in DB, standard KMP pattern, primitive-adapter provides Instant out of box.
Harder: two adapter types, LocalDate adapter hand-rolled, primitive-adapter dependency added.
