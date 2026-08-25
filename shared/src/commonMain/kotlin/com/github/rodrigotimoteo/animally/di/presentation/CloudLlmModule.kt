package com.github.rodrigotimoteo.animally.di.presentation

import com.github.rodrigotimoteo.animally.bridge.ObjCHidden
import com.github.rodrigotimoteo.animally.llm.cloud.CloudModelCatalog
import com.github.rodrigotimoteo.animally.presentation.settings.CloudLlmSettingsStore
import com.github.rodrigotimoteo.animally.presentation.settings.createPlatformCloudLlmSettings
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.dsl.module

/**
 * RUNTIME registration for the cloud AI settings store (toggle/model/base URL in
 * platform preferences, API key in secure storage). Manual DSL on purpose: the
 * annotations-generated module() extension is flaky under incremental compiles
 * (ThemeModule precedent), so platform initializers load THIS val directly.
 */
val cloudLlmModule =
    module {
        single<CloudLlmSettingsStore> { createPlatformCloudLlmSettings() }
    }

/**
 * Compile-time satisfier ONLY — never loaded at runtime. The Koin annotations
 * compiler requires an annotated provider to exist for every [SettingsViewModel]
 * constructor dependency; without this class it fails with KOIN-D001 because it
 * cannot see the manual DSL binding above.
 */
@Module
@ObjCHidden
class CloudLlmModule {
    @Single
    fun provideCloudLlmSettingsStore(): CloudLlmSettingsStore = createPlatformCloudLlmSettings()

    /** Compile-time satisfier for [SettingsViewModel]'s `cloudModelCatalog` param. */
    @Single
    fun provideCloudModelCatalog(httpClient: HttpClient): CloudModelCatalog = CloudModelCatalog(httpClient)
}
