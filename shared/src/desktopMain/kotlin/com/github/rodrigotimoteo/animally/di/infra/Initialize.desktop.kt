@file:Suppress("ktlint:standard:filename", "UNUSED_PARAMETER")

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.di.DesktopDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.dispatchers.DispatchersModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import com.github.rodrigotimoteo.animally.di.presentation.cloudLlmModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import com.github.rodrigotimoteo.animally.di.dispatchers.module as dispatchersModule
import com.github.rodrigotimoteo.animally.di.infra.module as appModule

/**
 * Desktop [initKoin] — mirrors the platform-independent production wiring:
 * no platform context or notification channel, but including the generated
 * application and dispatcher bindings required by the production graph.
 */
actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        modules(
            buildList {
                add(navigationEntryModule)
                add(AppModule().appModule())
                add(DispatchersModule().dispatchersModule())
                add(DesktopDatabaseModule().provide())
                add(QueriesModule().provide())
                add(PresentationModule().provide())
                add(cloudLlmModule)
                add(com.github.rodrigotimoteo.animally.llm.llmModule)
                add(com.github.rodrigotimoteo.animally.di.dictationModule)
                add(com.github.rodrigotimoteo.animally.di.settingsModule)
            },
        )
    }
