# ADR 0018: SQLDelight Migration Files From Day One

**Date**: 2026-07-11
**Status**: proposed

## Context
App stores medical records — data loss is unacceptable. Schema will change during development. Need migration strategy that preserves user data across app updates.

## Decision
Use .sqm migration files from day 1. Versioned migrations (v1→v2, v2→v3). deriveSchemaFromMigrations = true. Every schema change gets a migration file. Test migrations in androidHostTest.

## Alternatives considered
1. Recreate during dev, add migrations before release — fast iteration but risk of losing test data, migration files written retroactively may miss edge cases.
2. Schema dump + recreate — simple but loses all data on every schema change.
3. No migrations, never change schema — unrealistic.

## Consequences
Easier: data preserved across updates, production-safe from day 1, migration testing built into CI.
Harder: every schema change needs .sqm file, more discipline required during dev, slightly slower iteration.
