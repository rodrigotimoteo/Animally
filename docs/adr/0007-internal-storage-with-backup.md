# ADR 0007: Internal Storage with Explicit Backup Over Shared Storage

**Date**: 2026-07-11
**Status**: proposed

## Context
App stores file attachments (images, PDFs, X-rays). Need permissionless storage that survives app updates. User wants data to survive reinstalls via backup mechanism, not platform storage tricks.

## Decision
Store attachments in app-internal storage (Context.filesDir on Android, NSDocumentDirectory on iOS). Implement explicit backup/restore in Phase 3 (copy DB + files to user-chosen location). Cloud sync in Phase 6 handles cross-device.

## Alternatives considered
1. Shared documents (user-visible) — survives uninstall but Android 11+ scoped storage restrictions, requires SAF/permissions, no iOS equivalent.
2. Platform auto-backup (Android Auto Backup, iCloud) — 25MB Android limit, no control, unreliable timing.
3. External storage — deprecated on Android, not available on iOS.

## Consequences
Easier: no permissions, no platform storage fights, full control, offline-first aligned.
Harder: data lost on uninstall without manual backup, user must proactively backup, Phase 3 backup/restore required not optional.
