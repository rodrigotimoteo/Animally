@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.di.DesktopDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/**
 * Desktop [initKoin] — mirrors the iOS wiring: no platform context or
 * notification channel, just navigation, database, queries and presentation
 * modules.
 */
actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        modules(
            navigationEntryModule,
            DesktopDatabaseModule().provide(),
            QueriesModule().provide(),
            PresentationModule().provide(),
        )
    }
