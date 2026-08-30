# Improvement Pass Checklist

## Build recovery

- [x] Restore `CloudLlmConfig` import in the iOS cloud settings store.
- [x] Restore provider preset persistence on iOS.
- [x] Add isolated iOS tests for defaults, provider preset, and secure API-key handling.
- [x] Align Android/Desktop `ObjCHidden` annotation metadata.
- [x] Replace the order-dependent wipe fake test with explicit port tracking.
- [x] Add a native iOS PDF smoke test and remove the impossible string cast.
- [x] Add explicit native interop opt-ins and remove unnecessary sync-test assertions.
- [x] Rename the common RAG orchestration golden suite to avoid the Android-host class collision.

## Regression guard

- [x] Add iOS simulator Kotlin compilation to the checked-in pre-commit hook.
- [x] Make the hook installer configuration-cache compatible.
- [x] Refresh the installed local pre-commit hook.

## Verification

- [x] Focused iOS cloud settings tests pass.
- [x] Android and iOS simulator compilation passes.
- [x] Shared Android host and iOS simulator tests pass.
- [x] Detekt and KtLint pass.
- [x] Xcode simulator build passes.
- [x] Review the final diff for architecture boundary violations.
- [x] Commit the completed batch.
