package com.github.rodrigotimoteo.animally.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

actual fun initKoin(context: Any?): KoinApplication = startKoin {
    modules(
        IosDatabaseModule().provide(),
        QueriesModule().provide()
    )
}