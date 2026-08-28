# SonarQube

Animally uses the SonarScanner for Gradle (`org.sonarqube` `7.4.0.8496`) from
the root build. The scanner is opt-in so ordinary Android/KMP builds do not
need a SonarQube server.

## Run an analysis

Provide credentials through the environment or Gradle user properties; never
commit a token to this repository:

```sh
SONAR_HOST_URL=https://sonarcloud.io \
SONAR_TOKEN=your-token \
./gradlew quality \
  -Dsonar.organization=your-organization \
  -Dsonar.projectKey=your-project-key
```

For a local SonarQube server, set `SONAR_HOST_URL` to its URL and omit the
SonarCloud-only `organization` property. `./gradlew sonar` is also available;
`quality` is the preferred command because it makes the local checks and
report generation explicit.

The analysis command runs:

- Detekt and KtLint, including their Checkstyle-compatible reports;
- Android Lint for the debug variant;
- the existing Kover XML coverage report from JVM/Android/desktop tests; and
- the SonarQube scanner with `sonar.qualitygate.wait=true` by default.

The current source scope includes shared Kotlin (`commonMain`, `androidMain`,
`iosMain`, and `desktopMain`), the Android application, the iOS Swift host, and
their test sources. Generated code, build output, Xcode project/resource
containers, and preview assets are excluded.

## Recommended server policy

SonarQube rules and quality gates are server-side settings. Configure the
project with the built-in Sonar Way profiles for Kotlin and Swift, then create
or assign a gate with these conditions on **new code**:

| Condition | Threshold |
| --- | --- |
| New Bugs | 0 |
| New Vulnerabilities | 0 |
| Reliability Rating | A |
| Security Rating | A |
| Maintainability Rating | A |
| Security Hotspots Reviewed | 100% |
| Coverage | at least 80% |
| Duplicated Lines (%) | at most 3% |

This keeps legacy debt visible without making old findings block every change.
New security, reliability, and maintainability regressions remain blocking.
Do not mark a finding false-positive merely to pass the gate; document a real
exception in the SonarQube issue and, where useful, in an ADR.

Kover does not currently measure native iOS execution. iOS Swift and Kotlin
native code are therefore included in issue analysis but excluded from the
coverage denominator until an `xccov`/generic coverage import is added.

## Useful overrides

For diagnostics only, the quality-gate wait can be disabled without changing
the committed configuration:

```sh
./gradlew sonar -Psonar.qualitygate.wait=false
```

The project key is `rodrigotimoteo_Animally` by default and can be overridden by
`-Dsonar.projectKey=...` or `-Psonar.projectKey=...`.
