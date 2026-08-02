package com.github.rodrigotimoteo.animally.di.infra

import android.content.Context
import com.github.rodrigotimoteo.animally.di.AndroidDatabaseModule
import com.github.rodrigotimoteo.animally.di.database.QueriesModule
import com.github.rodrigotimoteo.animally.di.navigation.navigationEntryModule
import com.github.rodrigotimoteo.animally.di.presentation.PresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

actual fun initKoin(context: Any?): KoinApplication =
    startKoin {
        androidContext(context as Context)
        modules(
            navigationEntryModule,
            AndroidDatabaseModule().provide(),
            QueriesModule().provide(),
            PresentationModule().provide(),
        )
    }
