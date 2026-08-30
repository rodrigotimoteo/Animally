import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        iosTarget.binaries.all {
            linkerOpts("-U", "_OBJC_CLASS_\$_UIViewLayoutRegion")
        }

        // Kotlin/Native test executables are independent of the iosApp target, so they
        // cannot link the Swift implementations of these Objective-C shims. Compile the
        // inert test doubles into a target-specific archive and force-load it only into
        // the test binary. This keeps the native shared test gate runnable without changing
        // the production framework or app link.
        val iosTestStubSources =
            listOf(
                rootProject.file("shared/src/iosTest/objc/AnimallySyncShimStub.m"),
                rootProject.file("shared/src/iosTest/objc/FmLlmShimStub.m"),
            )
        val iosTestStubIncludeDir = rootProject.file("shared/src/nativeInterop/cinterop")
        val iosTestStubBuildDir =
            layout.buildDirectory
                .dir("iosTestStubs/${iosTarget.name}")
                .get()
                .asFile
        val iosTestStubObjectFiles =
            iosTestStubSources.map { source ->
                iosTestStubBuildDir.resolve("${source.nameWithoutExtension}.o")
            }
        val iosTestStubCompileTasks =
            iosTestStubSources.zip(iosTestStubObjectFiles).map { (source, objectFile) ->
                tasks.register<Exec>(
                    "compile${iosTarget.name.replaceFirstChar { it.uppercase() }}${source.nameWithoutExtension}IosTestStub",
                ) {
                    inputs.file(source)
                    inputs.dir(iosTestStubIncludeDir)
                    outputs.file(objectFile)
                    doFirst { objectFile.parentFile.mkdirs() }
                    commandLine(
                        "xcrun",
                        "--sdk",
                        if (iosTarget.name == "iosSimulatorArm64") "iphonesimulator" else "iphoneos",
                        "clang",
                        "-target",
                        if (iosTarget.name == "iosSimulatorArm64") {
                            "arm64-apple-ios15.0-simulator"
                        } else {
                            "arm64-apple-ios15.0"
                        },
                        "-fblocks",
                        "-I${iosTestStubIncludeDir.absolutePath}",
                        "-c",
                        source.absolutePath,
                        "-o",
                        objectFile.absolutePath,
                    )
                }
            }
        val iosTestStubArchive = iosTestStubBuildDir.resolve("libAnimallyIosTestStubs.a")
        val iosTestStubArchiveTask =
            tasks.register<Exec>("build${iosTarget.name.replaceFirstChar { it.uppercase() }}IosTestStubs") {
                dependsOn(iosTestStubCompileTasks)
                inputs.files(iosTestStubSources)
                outputs.file(iosTestStubArchive)
                doFirst { iosTestStubArchive.parentFile.mkdirs() }
                commandLine(
                    listOf(
                        "xcrun",
                        "--sdk",
                        if (iosTarget.name == "iosSimulatorArm64") "iphonesimulator" else "iphoneos",
                        "libtool",
                        "-static",
                        "-o",
                        iosTestStubArchive.absolutePath,
                    ) + iosTestStubObjectFiles.map { it.absolutePath },
                )
            }
        val iosDebugTestBinary = iosTarget.binaries.getTest("DEBUG")
        iosDebugTestBinary.linkerOpts("-force_load", iosTestStubArchive.absolutePath)
        tasks.named(iosDebugTestBinary.linkTaskName).configure {
            dependsOn(iosTestStubArchiveTask)
        }

        iosTarget.compilations.getByName("main") {
            cinterops {
                // Header-only cinterop binding to the Swift @objc shim (FmLlmShim).
                // The .def lives at src/nativeInterop/cinterop/FoundationModelsShim.def.
                create("FoundationModelsShim") {
                    // headers = FmLlmShim.h (referenced from the .def); add its directory to the
                    // include path with an absolute path so the cinterop finds it regardless of CWD.
                    compilerOpts("-I$rootDir/shared/src/nativeInterop/cinterop")
                }
                // The .def lives at src/nativeInterop/cinterop/CloudKitShim.def.
                create("CloudKitShim") {
                    compilerOpts("-I$rootDir/shared/src/nativeInterop/cinterop")
                }
            }
        }
    }

    jvm("desktop")

    android {
        namespace = "com.github.rodrigotimoteo.animally.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCRefinement")
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)

            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            implementation(libs.sqldelight.native.driver)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.sqldelight.driver.sqlite)
            implementation(libs.ktor.client.cio)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.koin.core)
            api(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(libs.navigation3.ui)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.datetime)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapter)

            implementation(libs.filekit.dialogs.compose)
            implementation(libs.kmpnotifier.local)

            implementation(libs.haze)
            implementation(libs.haze.blur)
            implementation(libs.haze.blur.materials)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.sqldelight.driver.sqlite)
        }
        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.driver.sqlite)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mokkery)
            implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

detekt {
    source.setFrom(files("src/commonMain/kotlin", "src/androidMain/kotlin", "src/iosMain/kotlin"))
}

ktlint {
    filter {
        exclude { element -> element.file.absolutePath.contains("/build/") }
    }
}

sqldelight {
    databases {
        create("AnimallyDatabase") {
            packageName.set("com.github.rodrigotimoteo.animally.data")
            deriveSchemaFromMigrations = true
        }
    }
}

// ---------------------------------------------------------------------------
// Kover code-coverage configuration
//
// Coverage is aggregated across the JVM unit-test targets:
//   androidHostTest + desktopTest
// iOS native tests are NOT measured (Kover only supports JVM/Android targets).
//
// Threshold: current measured merged line coverage (57.42% on 2026-08-04).
// production target: 90%
// Draft bump: 65% (Phase 5e gate) → 75% (Phase 5 final) — not enforced yet.
// Phase 5a/b/c keep 57 to avoid blocking parallel repo/test lanes; bump to
// 65/75 only after Delete-UC + repo coverage lands. Preview: ./gradlew :shared:koverVerify
val koverMinLineCoverage: Int = 57

kover {
    reports {
        filters {
            excludes {
                classes(
                    // SQLDelight-generated code (build/generated/sqldelight)
                    "com.github.rodrigotimoteo.animally.data.*Queries",
                    "com.github.rodrigotimoteo.animally.data.*Queries$*",
                    "com.github.rodrigotimoteo.animally.data.*AnimallyDatabase*",
                    "com.github.rodrigotimoteo.animally.data.*Migrations*",
                    // Compose Multiplatform resources generated code (Res class)
                    "*.generated.resources.*",
                    // Koin compiler-generated module class (@Module in AppModule.kt)
                    "com.github.rodrigotimoteo.animally.di.infra.ComGithubRodrigotimoteoAnimallyDiInfraAppModuleModuleKt",
                )
            }
        }
        verify {
            rule {
                minBound(koverMinLineCoverage)
            }
        }
    }
}
