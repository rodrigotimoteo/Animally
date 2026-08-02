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
}

kotlin {
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
    }

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
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.sqldelight.driver.sqlite)
        }
        iosTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mokkery)
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
