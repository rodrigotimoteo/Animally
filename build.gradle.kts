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
                xml.required.set(false)
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
