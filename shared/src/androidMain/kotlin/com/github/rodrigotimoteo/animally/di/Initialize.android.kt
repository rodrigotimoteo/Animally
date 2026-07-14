package com.github.rodrigotimoteo.animally.di

import android.content.Context
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.android.ext.koin.androidContext

actual fun initKoin(context: Any?): KoinApplication = startKoin {
    androidContext(context as Context)
    modules(
        AndroidDatabaseModule().provide(),
        QueriesModule().provide()
    )
}