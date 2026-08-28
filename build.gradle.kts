import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.sonarqube)
}

subprojects {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            toolVersion = "1.23.8"
            config.setFrom("$rootDir/config/detekt/detekt.yml")
            buildUponDefaultConfig = true
            parallel = true
            baseline = file("$rootDir/config/detekt/baseline/${project.name}.xml")
        }

        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            jvmTarget = JavaVersion.VERSION_17.toString()
            exclude("**/build/**")
            exclude("**/generated/**")
            reports {
                html.required.set(true)
                xml.required.set(true)
                sarif.required.set(false)
            }
        }

        tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }

        dependencies {
            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
        }
    }

    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        configure<KtlintExtension> {
            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.CHECKSTYLE)
            }
        }
    }
}

tasks.register("installGitHooks") {
    doLast {
        val hookDir = layout.projectDirectory.dir(".git/hooks")
        hookDir.asFile.mkdirs()
        layout.projectDirectory.file("gradle/pre-commit.sh").asFile.copyTo(
            hookDir.file("pre-commit").asFile,
            overwrite = true
        )
        hookDir.file("pre-commit").asFile.setExecutable(true)
        logger.lifecycle("✓ Pre-commit hook installed at .git/hooks/pre-commit")
    }
    notCompatibleWithConfigurationCache("InstallGitHooks uses file ops inside doLast")
}

tasks.register("detektBaselineAll") {
    description = "Generate detekt baseline for all subprojects"
    group = "verification"
    dependsOn(subprojects.map { "${it.path}:detektBaseline" })
}

// ---------------------------------------------------------------------------
// SonarQube configuration
//
// The scanner is intentionally opt-in: normal local builds remain independent
// of a SonarQube server, while `./gradlew sonar` and `./gradlew quality` run a
// complete, reproducible analysis when SONAR_HOST_URL/SONAR_TOKEN are present.
// SonarQube's quality profile and quality gate are configured on the server;
// config/sonar/README.md records the strict project policy used by this build.
sonar {
    properties {
        property(
            "sonar.projectKey",
            providers.gradleProperty("sonar.projectKey").orElse("rodrigotimoteo_Animally").get(),
        )
        property("sonar.projectName", "Animally")
        property("sonar.projectDescription", "iOS-first equine veterinary internship record app")
        property("sonar.sourceEncoding", "UTF-8")

        // KMP source sets are not all under the conventional src/main tree.
        // Keep this list explicit so shared business logic and the iOS host are
        // analyzed instead of silently disappearing from the project.
        property(
            "sonar.sources",
            listOf(
                "shared/src/commonMain/kotlin",
                "shared/src/androidMain/kotlin",
                "shared/src/iosMain/kotlin",
                "shared/src/desktopMain/kotlin",
                "androidApp/src/main/kotlin",
                "iosApp/iosApp",
            ),
        )
        property(
            "sonar.tests",
            listOf(
                "shared/src/commonTest/kotlin",
                "shared/src/androidHostTest/kotlin",
                "shared/src/desktopTest/kotlin",
                "shared/src/iosTest/kotlin",
                "iosApp/iosUITests",
            ),
        )

        // Generated/build artifacts and Xcode resource containers are not
        // maintainable source files and create noisy or duplicate findings.
        property(
            "sonar.exclusions",
            listOf(
                "**/build/**",
                "**/.gradle/**",
                "**/.kotlin/**",
                "**/generated/**",
                "**/*.xcodeproj/**",
                "**/*.xcassets/**",
                "**/Preview Content/**",
            ),
        )
        property(
            "sonar.test.exclusions",
            listOf(
                "**/build/**",
                "**/generated/**",
            ),
        )

        // Kover currently measures JVM/Android/desktop execution only. Keep
        // iOS Kotlin and Swift in issue analysis without treating missing native
        // coverage as zero until an xccov/generic coverage import is added.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "iosApp/iosApp/**",
                "shared/src/iosMain/**",
                "shared/src/iosTest/**",
                "**/generated/**",
            ),
        )

        // Import the checks already enforced locally instead of maintaining a
        // second, divergent set of style/security findings in SonarQube.
        property("sonar.kotlin.detekt.reportPaths", "**/build/reports/detekt/detekt.xml")
        property("sonar.kotlin.ktlint.reportPaths", "**/build/reports/ktlint/**/*.xml")
        property("sonar.androidLint.reportPaths", "androidApp/build/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "shared/build/reports/kover/report.xml")
        property("sonar.junit.reportPaths", "**/build/test-results/**/*.xml")

        // Fail the analysis command if the server-side quality gate fails.
        // This remains overridable for diagnostics with -Psonar.qualitygate.wait=false.
        property(
            "sonar.qualitygate.wait",
            providers.gradleProperty("sonar.qualitygate.wait").orElse("true").get(),
        )
    }
}

tasks.named("sonar") {
    // Scanner task ordering is explicit so imported reports are generated
    // before analysis. The existing full test suite remains part of Kover's
    // report task and therefore still gates the Sonar run.
    dependsOn(
        subprojects.flatMap { project ->
            listOf("${project.path}:detekt", "${project.path}:ktlintCheck")
        },
        ":androidApp:lintDebug",
        ":shared:koverXmlReport",
    )
}

tasks.register("quality") {
    group = "verification"
    description = "Runs local static analysis, Android lint, coverage, and the SonarQube quality gate."
    dependsOn("sonar")
}
