package com.github.rodrigotimoteo.animally.di.infra

import com.github.rodrigotimoteo.animally.di.IosDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        modules(
            navigationEntryModule,
            IosDatabaseModule().provide(),
            QueriesModule().provide(),
            PresentationModule().provide(),
        )
    }
