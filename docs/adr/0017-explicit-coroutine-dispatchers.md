# ADR 0017: Explicit Coroutine Dispatcher Strategy

**Date**: 2026-07-11
**Status**: proposed

## Context
SQLDelight does not auto-dispatch queries — they run on the calling thread. DB access on Main thread crashes Android. Need consistent dispatcher strategy across commonMain.

## Decision
expect val ioDispatcher: CoroutineDispatcher in commonMain. Android: Dispatchers.IO. iOS: Dispatchers.Default (no IO dispatcher on native). Repos wrap all DB calls in withContext(ioDispatcher). Flow queries use asFlow().mapToList(ioDispatcher) from sqldelight-coroutines.

## Alternatives considered
1. Let caller decide — repos are synchronous, callers wrap in withContext. Flexible but error-prone (forget a dispatcher = Main thread DB = crash).
2. SQLDelight transacter built-in dispatching — SQLDelight doesn't provide this.
3. Dispatchers.Main with withContext — wrong, Main is for UI, not IO.

## Consequences
Easier: predictable, explicit, what developer is used to, safe from Main thread crashes.
Harder: every repo method needs withContext, extra dispatcher parameter in constructors, iOS has no true IO dispatcher.
