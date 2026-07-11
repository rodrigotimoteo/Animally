# ADR 0016: Hybrid Koin Module Structure — Annotations + Explicit Modules

**Date**: 2026-07-11
**Status**: proposed

## Context
Koin 4.x supports both annotation-based DI (KSP) and explicit module declarations. Need to choose structure that minimizes boilerplate while maintaining explicit control over interface bindings and platform-specific singletons.

## Decision
Hybrid. @Single/@Factory annotations on impl classes (KSP generates bindings). Explicit @Module for interface→impl bindings (Repository → RepositoryImpl). Explicit DatabaseModule for SqlDriver + AnimallyDatabase (platform-specific, expect/actual). Koin K2 compiler plugin validates graph at compile time.

## Alternatives considered
1. Pure annotations — @Single on everything, KSP generates all. Minimal boilerplate but interface binding needs @Binds which is awkward in Koin.
2. Pure explicit modules — hand-written module{} blocks. Full control but more code, no compile-time validation without annotations.
3. No DI — manual construction. Non-starter for 18+ entities.

## Consequences
Easier: annotations reduce boilerplate, explicit modules for bindings that annotations can't express, K2 plugin catches missing bindings at compile time.
Harder: two DI styles in codebase, KSP adds build time, learning curve for Koin annotations.
