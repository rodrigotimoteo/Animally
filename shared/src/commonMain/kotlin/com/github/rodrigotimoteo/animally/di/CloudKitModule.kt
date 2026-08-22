package com.github.rodrigotimoteo.animally.di

import com.github.rodrigotimoteo.animally.domain.sync.SyncEngine
import com.github.rodrigotimoteo.animally.sync.cloudkit.CloudKitSyncEngineImpl
import com.github.rodrigotimoteo.animally.sync.cloudkit.CloudKitSyncSettings
import com.github.rodrigotimoteo.animally.sync.cloudkit.SyncCloudBridge
import com.github.rodrigotimoteo.animally.sync.cloudkit.createSyncCloudBridge
import org.koin.dsl.module

/**
 * CloudKit sync bindings (iOS only — wire this module from Initialize.ios).
 *
 * The [SyncEngine] binding here must be registered BEFORE the annotation
 * generated AppModule: Koin resolves duplicate types first-registered-wins,
 * so this module takes over the engine slot when present. While the
 * `cloud_enabled` flag is off (default), the engine stays inert and the app
 * behaves as without it.
 */
val cloudKitModule =
    module {
        single<SyncCloudBridge> { createSyncCloudBridge() }
        single { CloudKitSyncSettings(get()) }
        single<SyncEngine> {
            CloudKitSyncEngineImpl(
                bridge = get(),
                changeTracker = get(),
                registry = get(),
                database = get(),
                settings = get(),
            )
        }
    }
