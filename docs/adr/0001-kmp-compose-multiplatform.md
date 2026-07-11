# ADR 0001: KMP with Compose Multiplatform for Shared UI

**Date**: 2026-07-11
**Status**: proposed

## Context
Animally targets Android + iOS. Need maximum code sharing (90% target) with single vet, offline-first app. UI must be native-feeling on both platforms.

## Decision
Use Kotlin Multiplatform with Compose Multiplatform. Shared UI in commonMain, thin platform shells in androidApp/iosApp. 90/10 rule — 90% code in commonMain.

## Alternatives considered
1. Native SwiftUI + Jetpack Compose — full platform control but doubles UI work.
2. Flutter — single codebase but not Kotlin ecosystem, doesn't integrate with KMP domain layer.
3. React Native — JS bridge overhead, not Kotlin.

## Consequences
Easier: single UI codebase, shared ViewModels, shared navigation.
Harder: iOS Compose rendering has overhead, platform-specific APIs need expect/actual, some Compose features lag on iOS.
