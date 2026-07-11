# ADR 0019: Three Test Targets — commonTest, iosTest, androidHostTest

**Date**: 2026-07-11
**Status**: proposed

## Context
KMP app targets Android + iOS. Logic in commonMain must work on both platforms. Platform-specific code (SQLDelight drivers, Instant handling, file storage) needs testing on actual platforms.

## Decision
Three test source sets. commonTest: all logic tests (use cases, mappers, validation) — run on JVM, fastest. iosTest: same tests run on iOS simulator — catches platform-specific SQLDelight native driver issues, Instant/LocalDate handling. androidHostTest: JVM-based Android tests with in-memory SQLite — catches Android-specific issues without emulator.

## Alternatives considered
1. commonTest only — fastest CI but misses platform-specific bugs (native SQLite differences, Instant epoch handling on iOS).
2. commonTest + iosTest — skip Android host tests, rely on commonTest for Android. Misses Android-specific SQLDelight driver issues.
3. commonTest + androidHostTest — skip iOS tests in CI. Misses iOS-specific issues, run manually.

## Consequences
Easier: catches platform-specific bugs early, high confidence in KMP correctness.
Harder: slower CI (three test targets), iOS simulator setup in CI, more test infrastructure.
