package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.di.IosDatabaseModule
import com.github.rodrigotimoteo.animally.di.cloudKitModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.dispatchers.DispatchersModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import com.github.rodrigotimoteo.animally.di.dispatchers.module as dispatchersModule
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        modules(
            buildList {
                add(navigationEntryModule)
                // CloudKit sync engine — inert while cloud_enabled flag is off.
                // Must precede AppModule: first-registered SyncEngine binding wins.
                add(cloudKitModule)
                // Generated annotation module: @KoinViewModel/@Single definitions
                // (repos, use cases, view models, navigator).
                add(AppModule().appModule())
                add(DispatchersModule().dispatchersModule())
                add(IosDatabaseModule().provide())
                add(QueriesModule().provide())
                add(PresentationModule().provide())
                add(com.github.rodrigotimoteo.animally.llm.llmModule)
            },
        )
    }
