# ADR 0004: Navigation 3 for Type-Safe Routing

**Date**: 2026-07-11
**Status**: proposed

## Context
App has 20+ screens with nested navigation (PatientDetail → AddEdit screens). Need KMP navigation that works on Android + iOS with type-safe routes.

## Decision
Use Navigation 3 (navigation3-ui 1.1.1) from JetBrains. Sealed interface Route with @Serializable data classes/objects. Polymorphic SerializersModule for iOS. NavHost with when(route) dispatch.

## Alternatives considered
1. Voyager — KMP but less active, smaller community.
2. Decompose — KMP, mature, but different paradigm (component-based, steeper learning curve).
3. Navigation Compose (AndroidX) — Android-only.
4. Manual backstack management — error-prone, no type safety.

## Consequences
Easier: type-safe routes, KMP support, JetBrains-maintained, sealed class hierarchy.
Harder: Navigation 3 is relatively new (1.x), limited docs/examples, polymorphic serialization setup needed for iOS.
