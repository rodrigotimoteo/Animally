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
    // The root scanner creates a Sonar extension for every Gradle project. Keep
    // report properties on the module that owns each report so the scanner does
    // not rebase one module's paths into every other module.
    extensions.configure<org.sonarqube.gradle.SonarExtension> {
        properties {
            when (project.name) {
                "shared" -> {
                    property(
                        "sonar.kotlin.detekt.reportPaths",
                        project.file("build/reports/detekt/detekt.xml").absolutePath,
                    )
                    property(
                        "sonar.kotlin.ktlint.reportPaths",
                        project.fileTree(project.file("build/reports/ktlint")) {
                            include("**/*.xml")
                        },
                    )
                    property(
                        "sonar.junit.reportPaths",
                        listOf(
                            project.file("build/test-results/desktopTest"),
                            project.file("build/test-results/testAndroidHostTest"),
                        ).joinToString(",") { it.absolutePath },
                    )
                    property(
                        "sonar.coverage.jacoco.xmlReportPaths",
                        project.file("build/reports/kover/report.xml").absolutePath,
                    )
                }

                "androidApp" -> {
                    property(
                        "sonar.kotlin.detekt.reportPaths",
                        project.file("build/reports/detekt/detekt.xml").absolutePath,
                    )
                    property(
                        "sonar.kotlin.ktlint.reportPaths",
                        project.fileTree(project.file("build/reports/ktlint")) {
                            include("**/*.xml")
                        },
                    )
                    property(
                        "sonar.androidLint.reportPaths",
                        project.file("build/reports/lint-results-debug.xml").absolutePath,
                    )
                }
            }
        }
    }

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

tasks.register<Copy>("installGitHooks") {
    description = "Install the repository's pre-commit hook."
    group = "build setup"

    from(layout.projectDirectory.file("gradle/pre-commit.sh"))
    into(layout.projectDirectory.dir(".git/hooks"))
    rename { "pre-commit" }
    filePermissions {
        unix("755")
    }
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

        // The Gradle Sonar plugin discovers Android and KMP source sets from
        // their subprojects. Listing those directories again at the root would
        // index shared Kotlin files twice. Keep only the standalone iOS host
        // here; the Gradle modules provide their own source roots below.
        property(
            "sonar.sources",
            listOf(
                "iosApp/iosApp",
            ),
        )
        property(
            "sonar.tests",
            listOf(
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
                "**/*.png",
                "**/*.jpg",
                "**/*.jpeg",
                "**/*.gif",
                "**/*.webp",
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
                "shared/src/desktopMain/kotlin/com/github/rodrigotimoteo/animally/domain/notification/NotificationScheduler.desktop.kt",
                "**/generated/**",
            ),
        )

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
