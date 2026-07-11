# ADR 0003: Koin Over Hilt for Dependency Injection

**Date**: 2026-07-11
**Status**: proposed

## Context
App uses KMP. Hilt is Android-only (JVM annotation processor, Android-specific). Need DI that works across commonMain, androidMain, iosMain.

## Decision
Use Koin 4.2.2 with Koin Annotations 4.2.2 + Koin K2 Compiler Plugin 1.0.1. Hybrid approach: annotations on impl classes (@Single/@Factory), explicit @Module for interface→impl bindings, explicit DatabaseModule for SqlDriver.

## Alternatives considered
1. Hilt — Android-only, no KMP support. Non-starter.
2. Dagger — same limitation.
3. Manual DI — no graph validation, boilerplate.
4. Kotlin Inject (kotlin-inject) — KMP but smaller ecosystem, less tooling.

## Consequences
Easier: KMP-native, compile-time graph validation via K2 plugin, Hilt-like annotation style.
Harder: Koin less performant than Dagger (runtime resolution), annotation processor adds build time, smaller community than Hilt.
