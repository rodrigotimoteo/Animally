# ADR 0020: In-Memory DB for Repo Tests, Mock Repos for Use Case Tests

**Date**: 2026-07-11
**Status**: proposed

## Context
Need test doubles for repository and use case layers. In-memory DB tests real SQL but slower. Mock tests are fast but don't catch SQL errors.

## Decision
Both. In-memory SQLDelight DB (JdbcSqliteDriver :memory:) for repository integration tests — catches SQL syntax errors, type adapter bugs, migration issues. Mock PatientRepository interface for use case unit tests — fast, isolated, tests business logic only.

## Alternatives considered
1. In-memory DB only — realistic but slower, use cases coupled to DB.
2. Mock only — fast but doesn't catch SQL errors or adapter bugs, tests mock behavior not real behavior.
3. No repo tests, test via UI — slowest, flaky, hard to debug.

## Consequences
Easier: repo tests catch SQL/adapter bugs, use case tests are fast and focused, clear separation of concerns.
Harder: two test double strategies, more test infrastructure, in-memory SQLite may differ slightly from platform SQLite.
