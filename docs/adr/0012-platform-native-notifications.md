# ADR 0012: Platform-Native Notifications Over KMP-Notifier

**Date**: 2026-07-11
**Status**: proposed

## Context
Phase 4 requires local notifications for reminders (vaccination due, Coggins expiry, dentistry, custom). KMP-Notifier offers cross-platform API but maturity and feature coverage vary.

## Decision
Platform-native notifications via expect/actual. Android: AlarmManager + NotificationManager + WorkManager for scheduling. iOS: UNUserNotificationCenter with UNCalendarNotificationTrigger. Full platform control, no third-party dependency risk.

## Alternatives considered
1. KMP-Notifier — cross-platform, one API, but library maturity uncertain, feature gaps possible.
2. Firebase Cloud Messaging — requires network, conflicts with offline-first.
3. Defer to Phase 4 — decided to decide now to inform architecture.

## Consequences
Easier: full platform control, no third-party risk, platform-specific features accessible.
Harder: two implementations, more code, platform-specific scheduling differences (WorkManager vs UNUserNotificationCenter).
