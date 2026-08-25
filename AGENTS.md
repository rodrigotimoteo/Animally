# Animally Codex instructions

## Project shape

- This is a Kotlin Multiplatform project with `:androidApp` and `:shared` modules.
- The shared module has `iosArm64` and `iosSimulatorArm64` targets.
- The repository currently does not contain an iOS application host target, so do not claim that Animally's iOS UI was launched or tested unless an iOS host is added or an external host is explicitly provided.

## iOS simulator workflow

- Use the `ios-simulator` MCP for simulator inspection and UI interaction when the task requires it.
- Before simulator work, confirm that Xcode and an available simulator runtime/device are healthy.
- Prefer the simulator MCP for visual/UI evidence; use Gradle tasks for Kotlin compilation and tests.
- If CoreSimulatorService, Xcode, a runtime, or an app bundle is unavailable, report the exact blocker and continue with static or Gradle-based verification where useful.

## Verification

- For shared Kotlin changes, prefer focused Gradle tasks and the narrowest relevant test target.
- For iOS-specific changes, verify the relevant native target when practical and distinguish compilation evidence from simulator UI evidence.
